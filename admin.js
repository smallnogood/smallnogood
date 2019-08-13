'use strict'
const mongoose = require('mongoose')

const _ = require('lodash')
const axios = require('axios')
const Group = mongoose.model('Group')
const News = mongoose.model('News')
const User = mongoose.model('User')
const School = mongoose.model('School')
const Book = mongoose.model('Book')
const Organization = mongoose.model('Organization')
const Department = mongoose.model('Department')
const Conference = mongoose.model('Conference')
const ChiefCommend = mongoose.model('ChiefCommend')
const Guanggao = mongoose.model('Guanggao')
const Slide = mongoose.model('Slide')
const KeyProject = mongoose.model('KeyProject')
const ConferenceApplyConfig = mongoose.model('ConferenceApplyConfig')
const shortid = require('shortid32')
const config = require('../config/config')
const keys = require('../config/keys')
const OSS = require('ali-oss')
const utility = require('utility')
const co = require('co')
const xlsx = require('node-xlsx')
const emailTool = require('../helpers/email')
const ConferenceFileUser = mongoose.model('ConferenceFileUser')
const ConferenceApply = mongoose.model('ConferenceApply')
const ConferenceApplyFile = mongoose.model('ConferenceApplyFile')
const RealNameTeacher = mongoose.model('RealNameTeacher')
const FileMeta = mongoose.model('FileMeta')
const excelexport = require('excel-export')
const fs = require('fs')
const tool = require('../helpers/tool')
const sso = require('../helpers/sso')
const dayjs = require('dayjs')
const qn = require('../helpers/qn')
const path = require('path')
const log = require('tracer').colorConsole()
const auth = require('../auth/auth.service')
let request = require('request')
let archiver = require('archiver')
const admin_api = require('../api/v1/admin')

require('mongoose-paginater')

let uploadFileToAliyun = function(path, req) {
  try {
    let client = new OSS({
      region: keys.aliyun.region,
      accessKeyId: keys.aliyun.accessKeyId,
      accessKeySecret: keys.aliyun.accessKeySecret,
      bucket: keys.aliyun.bucket
    })
    if (!req.file) return { status: 'error' }
    let filename = req.file.filename + req.file.originalname.substr(req.file.originalname.lastIndexOf('.'))
    co(function*() {
      var result = yield client.put(path + filename, req.file.path)
    }).catch(function(err) {
      console.log(err)
      return { status: 'error' }
    })
    let fileMeta = new FileMeta()
    fileMeta._id = shortid.generate()
    fileMeta.filename = path + filename
    fileMeta.origName = req.file.originalname
    fileMeta.bucket = keys.aliyun.bucket
    fileMeta.storePlace = config.store.ali
    fileMeta.mimeType = req.file.mimetype
    fileMeta.origSize = req.file.size
    fileMeta.creator = req.user
    if (req.body.type && req.body.type == 'conference_photo') {
      fileMeta.title = req.body.title
    }
    fileMeta.save()
    return { status: 'success', fileId: fileMeta._id }
  } catch (err) {
    console.log(err)
    return { status: 'error' }
  }
}

exports.uploadFileToAliyun = uploadFileToAliyun

let uploadFileToQn = async function(req, id) {
  try {
    let qnFM
    let qnfile = tool.string2Json(req.body.fileqn)
    let tmpfile = tool.string2Json(req.body.filetmp)
    if (qnfile && tmpfile) {
      // 有文件 保存一下
      let ext = path.extname(qnfile.key).toLowerCase()
      let filename = utility.md5(qnfile.hash) + ext
      log.info('qn', qnfile, 'tmp', tmpfile)
      if (id && id != '') qnFM = await FileMeta.findById(id)
      if (!qnFM) {
        qnFM = new FileMeta()
        qnFM._id = shortid.generate()
      }
      qnFM.filename = filename
      qnFM.origName = tmpfile.name
      qnFM.mimeType = tmpfile.type
      qnFM.origSize = tmpfile.origSize
      qnFM.tmpKey = qnfile.key
      // qnFM.type = config.type.qn
      qnFM.bucket = keys.qn.publicbucket
      qnFM.creator = req.user
      if (req.body.type && req.body.type == 'file') {
        qnFM.status = config.status.normal
        qnFM.title = req.body.title
      }
      if (req.body.type && req.body.type == 'conference_photo') {
        qnFM.title = req.body.title
      }
      qnFM = await qnFM.save()
      qn.tmpFileStoreToPublic(qnFM)
    } else {
      return { status: 'error' }
    }
    return { status: 'success', fileId: qnFM._id }
  } catch (err) {
    console.log(err)
    return { status: 'error' }
  }
}

exports.newsColumnList = async function(req, res, next) {
  res.renderPjax('admin/column/manage_column')
}

exports.addColumn = (req, res, next) => {
  res.renderPjax('admin/column/add_column', {
    parent: req.query.parent ? req.query.parent : '#'
  })
}

exports.getOrgList = async function(req, res, next) {
  let orgs = await Organization.find({ status: config.status.normal })
  res.renderPjax('admin/column/inc/org_list', {
    orgs: orgs
  })
}

exports.getColumnDetail = async function(req, res, next) {
  let group = await Group.findById(req.query.id)
    .populate('editor')
    .populate('admin')
    .populate('superAdmin')
  res.renderPjax('admin/column/inc/column_detail', {
    group: group
  })
}

exports.addColumnDo = async function(req, res, next) {
  try {
    let group = new Group()
    group._id = shortid.generate()
    group.title = req.body.title
    group.columnId = req.body.columnId
    group.parent = req.body.parent
    group.editor = req.body.editor
    group.admin = req.body.publisher
    group.showPlace = 'hep'
    group.status = config.status.normal
    group.superAdmin = req.body.checker
    await group.save()

    res.json({
      status: 'success'
    })
  } catch (err) {
    console.log(err)
    res.json({
      status: 'error'
    })
  }
}

exports.delColumn = async function(req, res, next) {
  await Group.findByIdAndUpdate(req.query._id, { status: config.status.delete })
  res.send({ callBack: '0' })
}

exports.editColumn = async function(req, res, next) {
  let group = await Group.findById(req.query.id)
    .populate('editor')
    .populate('admin')
    .populate('superAdmin')
  res.renderPjax('admin/column/edit_column', {
    group: group
  })
}

exports.editColumnDo = async function(req, res, next) {
  try {
    let group = new Group()
    group._id = req.body._id
    group.title = req.body.title
    group.columnId = req.body.columnId
    group.parent = req.body.parent
    group.editor = req.body.editor
    group.admin = req.body.publisher
    group.superAdmin = req.body.checker
    await Group.findOneAndUpdate({ _id: group._id }, group, { up: 'up' })

    res.json({
      status: 'success'
    })
  } catch (err) {
    console.log(err)
    res.json({
      status: 'error'
    })
  }
}

exports.newsManage = async function(req, res, next) {
  console.log(req.session.captcha)
  res.renderPjax('admin/news/manage_news', {
    status: req.query.status ? req.query.status : 'normal'
  })
}

exports.newsManageCenter = async function(req, res, next) {
  res.renderPjax('admin/news/manage_news_center', {
    status: req.query.status ? req.query.status : 'normal'
  })
}

exports.addNews = (req, res, next) => {
  let token = qn.uptoken('node2d-public')
  res.renderPjax('admin/news/add_news', { token: token })
}

exports.addNewsDo = async function(req, res, next) {
  try {
    let news = new News(req.body.news)
    news._id = shortid.generate()
    news.showPlace = 'hep'
    news.status = config.status.normal
    news.creator = req.user
    await news.save()

    res.redirect('/admin/newsManageHep')
  } catch (err) {
    console.log(err)
  }
}

// @todo 加上news为空的判断
exports.editNews = async function(req, res, next) {
  let news = await News.findById(req.query.id)
    .populate('group01')
    .populate('group02')
    .exec()
  let group02 = ''
  if (news && null != news.group02) group02 = news.group02._id
  let token = qn.uptoken('node2d-public')
  res.renderPjax('admin/news/edit_news', {
    news: news,
    group02: group02,
    token: token
  })
}

exports.editNewsDo = async function(req, res, next) {
  try {
    let news = await News.findById(req.body.news._id)
    news = _.assignIn(news, req.body.news)
    await news.save()
    res.redirect('/admin/newsManageHep?status=' + news.status)
  } catch (err) {
    console.log(err)
  }
}

exports.commendNews = async function(req, res, next) {
  try {
    let news_old = await News.findById(req.body.news_id1)
    let news = new News()
    news._id = shortid.generate()
    news.showPlace = 'hep'
    news.checker = ''
    news.creator = req.user
    news.group01 = req.body.group01
    news.group02 = req.body.group02
    news.status = config.status.checked
    news.displayTitle = news_old.displayTitle
    news.title = news_old.title
    news.subTitle = news_old.subTitle
    news.author = news_old.author
    news.desc = news_old.desc
    news.content = news_old.content
    news.keywords = news_old.keywords
    news.brief = news_old.brief
    news.from = news_old.from
    news.organization = news_old.organization
    news.attachments = news_old.attachments
    news.cover = news_old.cover
    news_old.isRecommend = 'yes'
    await news.save()
    await news_old.save()
    res.redirect('/admin/newsManageCenter?status=published')
  } catch (err) {
    console.log(err)
  }
}

exports.addNewsCenter = (req, res, next) => {
  let token = qn.uptoken('node2d-public')
  res.renderPjax('admin/news/add_news_center', { token: token })
}

exports.addNewsCenterDo = async function(req, res, next) {
  try {
    let news = new News(req.body.news)
    news._id = shortid.generate()
    news.status = config.status.normal
    news.showPlace = 'center'
    news.organization = req.user.organization
    news.creator = req.user
    await news.save()

    res.redirect('/admin/newsManageCenter')
  } catch (err) {
    console.log(err)
  }
}

exports.editNewsCenter = async function(req, res, next) {
  let news = await News.findById(req.query.id).exec()
  res.renderPjax('admin/news/edit_news_center', {
    news: news
  })
}

exports.editNewsCenterDo = async function(req, res, next) {
  try {
    let news = new News(req.body.news)
    news = await News.findOneAndUpdate({ _id: news._id }, news, { up: 'up' })
    res.redirect('/admin/newsManageCenter?status=' + news.status)
  } catch (err) {
    console.log(err)
  }
}

exports.updateNewsStatus = async function(req, res, next) {
  try {
    if (req.body.status != 'checked') {
      if (req.body.status == 'published')
        await News.findByIdAndUpdate(req.body._id, { status: req.body.status, checker: req.user, publishDate: tool.now() })
      else await News.findByIdAndUpdate(req.body._id, { status: req.body.status, checker: req.user })
    } else await News.findByIdAndUpdate(req.body._id, { status: req.body.status, checker: '' })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.deleteCover = async function(req, res, next) {
  try {
    await News.findByIdAndUpdate(req.body._id, { cover: '',hasCover:'no' })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.uploadNewsCover = async function(req, res, next) {
  try {
    let news = await News.findById(req.body.news_id).exec()
    let result = await uploadFileToQn(req, news.cover)
    if (result.status == 'success') {
      news.cover = result.fileId
      news.hasCover = 'yes'
      await news.save()
    } else {
      req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '上传失败。')
  }
  res.redirect(req.body.news_url)
}

exports.addFileDo = async function(req, res, next) {
  try {
    let result = await uploadFileToQn(req, '')
    if (result.status != 'success') {
      req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '上传失败。')
  }
  res.redirect('/admin/fileManage')
}

exports.newsFileList = async function(req, res, next) {
  let news = await News.findById(req.query.id)
    .populate('attachments')
    .exec()

  res.renderPjax('admin/news/inc/news_files', {
    news: news
  })
}

exports.uploadNewsFile = async function(req, res, next) {
  try {
    let news = await News.findById(req.body.file_news_id).exec()

    let result = await uploadFileToAliyun(config.oss_url.news_file, req)
    if (result.status == 'success') {
      let attachments = news.attachments
      attachments.push.apply(attachments, [result.fileId])
      news.attachments = attachments
      await news.save()
    } else {
      req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    res.send({ callBack: '0' })
  }
  res.send({ callBack: '1' })
}

exports.deleteNewsFile = async function(req, res, next) {
  try {
    await News.findOneAndUpdate(
      {
        _id: req.query.news_id
      },
      {
        $pull: {
          attachments: req.query._id
        }
      },
      {
        up: 'up'
      }
    )
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.index = async function(req, res, next) {
  log.info(req.user)
  res.renderPjax('admin/index')
}

exports.userManageEditor = async function(req, res, next) {
  res.renderPjax('admin/user/manage_user_editor')
}

exports.userManageReg = async function(req, res, next) {
  res.renderPjax('admin/user/manage_user_reg')
}

exports.schoolManage = async function(req, res, next) {
  res.renderPjax('admin/other/manage_school')
}

exports.fileManage = async function(req, res, next) {
  res.renderPjax('admin/other/manage_file')
}

exports.realNameManage = async function(req, res, next) {
  res.renderPjax('admin/other/manage_realName', {
    status: req.query.status ? req.query.status : config.status.checked
  })
}

exports.orgManage = async function(req, res, next) {
  res.renderPjax('admin/other/manage_org')
}

exports.userManageLeader = async function(req, res, next) {
  res.renderPjax('admin/user/manage_user_leader')
}

exports.departmentManage = async function(req, res, next) {
  res.renderPjax('admin/other/manage_department')
}

exports.changeUserOrg = async function(req, res, next) {
  try {
    await User.findByIdAndUpdate(req.body.user_id, { organization: req.body.department_co, roles: [config.role.editor], department: [] })
  } catch (err) {
    console.log(err)
    req.flash('notice', '出错了')
  }
  res.redirect('/admin/userManageEditor')
}

exports.setUserAdmin = async function(req, res, next) {
  try {
    if (req.body.type == 'add') {
      await User.findOneAndUpdate({ _id: req.body.user_id }, { $addToSet: { roles: { $each: [config.role.leader] } } })
    } else {
      await User.findOneAndUpdate(
        {
          _id: req.body.user_id
        },
        {
          $pull: {
            roles: config.role.leader
          }
        },
        {
          up: 'up'
        }
      )
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '出错了')
  }
  res.redirect('/admin/userManageEditor')
}

exports.changeUserStatus = async function(req, res, next) {
  try {
    await User.findByIdAndUpdate(req.body._id, { status: req.body.status })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.addUserDo = async function(req, res, next) {
  try {
    let user = new User(req.body.user)
    user.status = config.status.normal
    user.roles = [config.role.editor]
    await user.save()

    res.redirect('/admin/userManageEditor')
  } catch (err) {
    console.log(err)
  }
}

exports.getDepList = async function(req, res, next) {
  let deps = await Department.find({ organization: req.user.organization, status: config.status.normal })
  res.renderPjax('admin/user/inc/dep_list', {
    deps: deps
  })
}

exports.saveDepartment = async function(req, res, next) {
  try {
    await User.findByIdAndUpdate(req.body._id, { department: req.body.deps })
    res.send({ callBack: '0' })
  } catch (err) {
    console.log(err)
    res.send({ callBack: '1' })
  }
}

exports.getUserInfo = async function(req, res, next) {
  try {
    let user = await User.findById(
      req.query._id,
      '_id realname nickname  email avatar status role department updatedAt username organization roles role createdAt school isRealName'
    )
    res.send({ callBack: '0', user: user })
  } catch (err) {
    console.log(err)
    res.send({ callBack: '1' })
  }
}

exports.getAuthList = async function(req, res, next) {
  let org = await Organization.findById(req.user.organization)
  res.renderPjax('admin/user/inc/auth_list', {
    auths: org.roles
  })
}

exports.saveRoles = async function(req, res, next) {
  try {
    await User.findByIdAndUpdate(req.body._id, { roles: req.body.roles })
    res.send({ callBack: '0' })
  } catch (err) {
    console.log(err)
    res.send({ callBack: '1' })
  }
}

exports.addSchoolDo = async function(req, res, next) {
  try {
    let school = new School(req.body.school)
    school._id = shortid.generate()
    school.status = config.status.normal
    await school.save()

    res.redirect('/admin/schoolManage')
  } catch (err) {
    console.log(err)
  }
}

exports.editSchoolDo = async function(req, res, next) {
  try {
    let school = new School(req.body.school)
    await School.findOneAndUpdate({ _id: school._id }, school, { up: 'up' })

    res.redirect('/admin/schoolManage')
  } catch (err) {
    console.log(err)
  }
}

exports.deleteSchool = async function(req, res, next) {
  try {
    await School.findByIdAndUpdate(req.body._id, { status: config.status.delete })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.addDepDo = async function(req, res, next) {
  try {
    let dep = new Department(req.body.dep)
    dep._id = shortid.generate()
    dep.status = config.status.normal
    await dep.save()

    res.redirect('/admin/departmentManage')
  } catch (err) {
    console.log(err)
  }
}

exports.editDepDo = async function(req, res, next) {
  try {
    let dep = new Department(req.body.dep)
    await Department.findOneAndUpdate({ _id: dep._id }, dep, { up: 'up' })

    res.redirect('/admin/departmentManage')
  } catch (err) {
    console.log(err)
  }
}

exports.deleteDep = async function(req, res, next) {
  try {
    await Department.findByIdAndUpdate(req.body._id, { status: config.status.delete })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.addOrgDo = async function(req, res, next) {
  try {
    let org = new Organization(req.body.org)
    org._id = shortid.generate()
    org.status = config.status.normal
    await org.save()

    res.redirect('/admin/orgManage')
  } catch (err) {
    console.log(err)
  }
}

exports.editOrgDo = async function(req, res, next) {
  try {
    let org = new Organization(req.body.org)
    await Organization.findOneAndUpdate({ _id: org._id }, org, { up: 'up' })

    res.redirect('/admin/orgManage')
  } catch (err) {
    console.log(err)
  }
}

exports.deleteOrg = async function(req, res, next) {
  try {
    await Organization.findByIdAndUpdate(req.body._id, { status: config.status.delete })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.conferenceManage = async function(req, res, next) {
  res.renderPjax('admin/conference/manage_conference', {
    status: req.query.status ? req.query.status : 'normal'
  })
}

exports.addConference = async function(req, res, next) {
  try {
    let conference = new Conference(req.body.conference)
    conference._id = shortid.generate()
    conference.status = config.status.normal
    conference.creator = req.user
    conference.organization = req.user.organization
    await conference.save()

    res.redirect('/admin/conferenceManage')
  } catch (err) {
    console.log(err)
  }
}

exports.editConference = async function(req, res, next) {
  try {
    let conference = new Conference(req.body.conference)
    conference = await Conference.findOneAndUpdate({ _id: conference._id }, conference, { up: 'up' })

    res.redirect('/admin/conferenceManage?status=' + conference.status)
  } catch (err) {
    console.log(err)
  }
}

exports.updateConferenceStatus = async function(req, res, next) {
  try {
    await Conference.findByIdAndUpdate(req.body._id, { status: req.body.status })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.uploadConferenceCover = async function(req, res, next) {
  try {
    let conference = await Conference.findById(req.body.conference_id).exec()
    let result = await uploadFileToQn(req, conference.cover)
    if (result.status == 'success') {
      conference.cover = result.fileId
      await conference.save()
    } else {
      req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '上传失败。')
  }
  res.redirect('/admin/conferenceManage?status=' + req.body.conference_status)
}

exports.conferenceNews = async function(req, res, next) {
  res.renderPjax('admin/conference/conference_news', {
    conference_id: req.query.id ? req.query.id : ''
  })
}

exports.conferencePhoto = async function(req, res, next) {
  let conference = await Conference.findOne({ _id: req.query.id }).populate('photos')
  res.renderPjax('admin/conference/conference_photo', {
    conference: conference
  })
}

exports.addNewsConferenceDo = async function(req, res, next) {
  try {
    // 修改一下 把notice 直接放在会议的对象里面 取出比较方便 也省了一个接口
    let conference = await Conference.findById(req.body.news.conference)
    if (!conference) {
      log.error('conference not exist')
      // 这个地方错误没有处理 错误的统一处理 另外这种 同意都成为接口操作  不再处理页面间跳转
      res.redirect('/admin/conferenceNews?id=' + req.body.news.conference)
    }
    let news = new News(req.body.news)
    news._id = shortid.generate()
    news.status = config.status.normal
    news.showPlace = 'conference'
    news.organization = req.user.organization
    news.creator = req.user
    await news.save()
    conference.notice.addToSet(news)
    await conference.save()

    res.redirect('/admin/conferenceNews?id=' + news.conference)
  } catch (err) {
    // 这个报错
    console.log(err)
  }
}

exports.editNewsConferenceDo = async function(req, res, next) {
  try {
    let news = new News(req.body.news)
    news = await News.findOneAndUpdate({ _id: news._id }, news, { up: 'up' })
    res.redirect('/admin/conferenceNews?id=' + news.conference)
  } catch (err) {
    console.log(err)
  }
}

exports.conferenceRegConfig = async function(req, res, next) {
  res.renderPjax('admin/conference/reg_config', {
    conference_id: req.query.id
  })
}

exports.saveConferenceRegConfigDo = async function(req, res, next) {
  try {
    let regConfig
    regConfig = await ConferenceApplyConfig.find({ conference: req.body.regConfig.conference }).exec()
    if (regConfig.length != 0) {
      await ConferenceApplyConfig.findOneAndUpdate({ _id: regConfig[0]._id }, req.body.regConfig, { up: 'up' })
    } else {
      regConfig = new ConferenceApplyConfig(req.body.regConfig)
      regConfig._id = shortid.generate()
      await regConfig.save()
    }
    res.send({ callBack: '0' })
  } catch (err) {
    console.log(err)
    res.send({ callBack: '1' })
  }
}

exports.conferenceSlide = async function(req, res, next) {
  let page = req.query.page ? req.query.page : 1
  var options = {
    classNameSpace: '',
    perPage: 20,
    page: page,
    firstText: '首页',
    lastText: '末页',
    totalText: '%d / %d 共 %d 条',
    query: { id: req.query.id }
  }

  var query = Slide.find({ conference: req.query.id, status: config.status.normal }).populate('cover')

  query.paginater(options, function(err, result) {
    res.renderPjax('admin/conference/conference_slide', {
      conference_id: req.query.id ? req.query.id : '',
      results: result
    })
  })
}

exports.addSlideConference = async function(req, res, next) {
  try {
    let slide = new Slide(req.body.slide)
    slide._id = shortid.generate()
    slide.status = config.status.normal
    slide.showPlace = 'conference'
    slide.creator = req.user

    let result = await uploadFileToAliyun(config.oss_url.slide_cover, req)
    if (result.status == 'success') {
      slide.cover = result.fileId
      await slide.save()
    } else {
      req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '操作失败。')
  }
  res.redirect('/admin/conferenceSlide?id=' + req.body.slide.conference)
}

exports.editSlideConference = async function(req, res, next) {
  let slide = req.body.slide
  try {
    let result = await uploadFileToAliyun(config.oss_url.slide_cover, req)
    if (result.status == 'success') {
      slide.cover = result.fileId
      slide = await Slide.findOneAndUpdate({ _id: slide._id }, slide, { up: 'up' })
    } else {
      slide = await Slide.findOneAndUpdate({ _id: slide._id }, slide, { up: 'up' })
      // req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '操作失败。')
  }
  res.redirect('/admin/conferenceSlide?id=' + slide.conference)
}

exports.deleteSlide = async function(req, res, next) {
  try {
    await Slide.findByIdAndUpdate(req.body._id, { status: req.body.status ? req.body.status : config.status.delete })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.conferenceFileList = async function(req, res, next) {
  let conference = await Conference.findById(req.query.id)
    .populate('attachments')
    .exec()

  res.renderPjax('admin/conference/inc/conference_files', {
    conference: conference
  })
}

exports.deleteConferenceFile = async function(req, res, next) {
  try {
    await Conference.findOneAndUpdate(
      {
        _id: req.query.conference_id
      },
      {
        $pull: {
          attachments: req.query._id
        }
      },
      {
        up: 'up'
      }
    )
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.uploadConferenceFile = async function(req, res, next) {
  try {
    let conference = await Conference.findById(req.body.file_conference_id).exec()

    let result = await uploadFileToAliyun(config.oss_url.news_file, req)
    if (result.status == 'success') {
      let attachments = conference.attachments
      attachments.push.apply(attachments, [result.fileId])
      conference.attachments = attachments
      await conference.save()
    } else {
      req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    res.send({ callBack: '0' })
  }
  res.send({ callBack: '1' })
}

exports.importConferenceFileUser = async function(req, res, next) {
  let excel = xlsx.parse(req.file.path)
  let data = excel[0].data
  for (let index in data) {
    if (data[index][0]) {
      let conferenceFileUser = new ConferenceFileUser()
      conferenceFileUser._id = shortid.generate()
      conferenceFileUser.realname = data[index][0]
      conferenceFileUser.email = data[index][1]
      conferenceFileUser.conference = req.body.file_conference_id
      await conferenceFileUser.save()
    }
  }
  res.send({ callBack: '1' })
}

exports.conferenceApplyUser = async function(req, res, next) {
  res.renderPjax('admin/conference/conference_apply_user', {
    conference_id: req.query.id ? req.query.id : ''
  })
}

exports.conferenceApplyFile = async function(req, res, next) {
  res.renderPjax('admin/conference/conference_apply_file', {
    conference_id: req.query.id ? req.query.id : ''
  })
}

exports.importConferenceApplyUser = async function(req, res, next) {
  let excel = xlsx.parse(req.file.path)
  let data = excel[0].data
  for (let index in data) {
    if (data[index][0]) {
      let conferenceApply = new ConferenceApply()
      conferenceApply._id = shortid.generate()
      conferenceApply.name = data[index][0]
      conferenceApply.email = data[index][1]
      conferenceApply.mobile = data[index][2]
      conferenceApply.organization = data[index][3]
      conferenceApply.status = config.status.normal
      conferenceApply.conference = req.body.conference_id
      await conferenceApply.save()
    }
  }
  res.send({ callBack: '1' })
}

exports.deleteConferenceApplyUser = async function(req, res, next) {
  try {
    await ConferenceApply.findByIdAndUpdate(req.body._id, { status: req.body.status })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.deleteConferenceApplyFile = async function(req, res, next) {
  try {
    await ConferenceApplyFile.findByIdAndUpdate(req.body._id, { status: req.body.status })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.exportConferenceApplyUser = async (req, res, next) => {
  let randnum = tool.getOutTradeNo() + '.xlsx'
  let dir = 'assets/tmp'
  if (!fs.existsSync(dir)) fs.mkdirSync(dir)
  let conf1 = {}
  conf1.cols = [
    {
      caption: '姓名',
      type: 'string'
    },
    {
      caption: '邮箱',
      type: 'string'
    },
    {
      caption: '手机',
      type: 'string'
    },
    {
      caption: '职称',
      type: 'string'
    },
    {
      caption: '性别',
      type: 'string'
    },
    {
      caption: '职务',
      type: 'string'
    },
    {
      caption: '专业方向',
      type: 'string'
    },
    {
      caption: '所授课程1',
      type: 'string'
    },
    {
      caption: '所授课程2',
      type: 'string'
    },
    {
      caption: '传真',
      type: 'string'
    },
    {
      caption: '座机',
      type: 'string'
    },
    {
      caption: '单位',
      type: 'string'
    },
    {
      caption: '通信地址',
      type: 'string'
    },
    {
      caption: '邮编',
      type: 'string'
    },
    {
      caption: '院系',
      type: 'string'
    },
    {
      caption: '教研室',
      type: 'string'
    },
    {
      caption: '住宿要求',
      type: 'string'
    },
    {
      caption: '到会时间',
      type: 'string'
    },
    {
      caption: '离会时间',
      type: 'string'
    },
    {
      caption: 'qq',
      type: 'string'
    },
    {
      caption: '期望建议',
      type: 'string'
    },
    {
      caption: '到会航班或车次',
      type: 'string'
    },
    {
      caption: '离会航班或车次',
      type: 'string'
    },
    {
      caption: '参会人员身份',
      type: 'string'
    },
    {
      caption: '发票抬头',
      type: 'string'
    },
    {
      caption: '发票内容',
      type: 'string'
    },
    {
      caption: '发票备注',
      type: 'string'
    },
    {
      caption: '学习方式',
      type: 'string'
    },
    {
      caption: '是否提交微课',
      type: 'string'
    },
    {
      caption: '微课题目',
      type: 'string'
    },
    {
      caption: '是否提交论文',
      type: 'string'
    },
    {
      caption: '论文标题',
      type: 'string'
    },
    {
      caption: '论文类型',
      type: 'string'
    },
    {
      caption: '已汇款',
      type: 'string'
    },
    {
      caption: '中国细胞生物学学会会员号',
      type: 'string'
    },
    {
      caption: '自定义字段1',
      type: 'string'
    },
    {
      caption: '自定义字段2',
      type: 'string'
    },
    {
      caption: '自定义字段3',
      type: 'string'
    },
    {
      caption: '申请时间',
      type: 'string'
    }
  ]
  conf1.name = 'order'
  conf1.rows = {}

  let earray1 = []

  let conference = req.query.conference ? req.query.conference : '#'

  let list = await ConferenceApply.find({ conference: conference, status: config.status.normal }).exec()

  let tmp

  for (let i in list) {
    tmp = []
    let name = list[i].name ? list[i].name : ''
    let email = list[i].email ? list[i].email : ''
    let mobile = list[i].mobile ? list[i].mobile : ''
    let title = list[i].title ? list[i].title : ''
    let gender = list[i].gender ? list[i].gender : ''
    let position = list[i].position ? list[i].position : ''
    let subject = list[i].subject ? list[i].subject : ''
    let course1 = list[i].course1 ? list[i].course1 : ''
    let course2 = list[i].course2 ? list[i].course2 : ''
    let fax = list[i].fax ? list[i].fax : ''
    let telephone = list[i].telephone ? list[i].telephone : ''
    let organization = list[i].organization ? list[i].organization : ''
    let address = list[i].address ? list[i].address : ''
    let postCode = list[i].postCode ? list[i].postCode : ''
    let department = list[i].department ? list[i].department : ''
    let className = list[i].class ? list[i].class : ''
    let hotel = list[i].hotel ? list[i].hotel : ''
    let startDate = list[i].startDate ? list[i].startDate : ''
    let endDate = list[i].endDate ? list[i].endDate : ''
    let qq = list[i].qq ? list[i].qq : ''
    let suggest = list[i].suggest ? list[i].suggest : ''
    let startVehicleNo = list[i].startVehicleNo ? list[i].startVehicleNo : ''
    let endVehicleNo = list[i].endVehicleNo ? list[i].endVehicleNo : ''
    let role = list[i].role ? list[i].role : ''
    let invoiceTitle = list[i].invoiceTitle ? list[i].invoiceTitle : ''
    let invoiceContent = list[i].invoiceContent ? list[i].invoiceContent : ''
    let invoiceBrief = list[i].invoiceBrief ? list[i].invoiceBrief : ''
    let studyMode = list[i].studyMode ? list[i].studyMode : ''
    let hasMicroCourse = list[i].hasMicroCourse ? list[i].hasMicroCourse : ''
    let microCourseTitle = list[i].microCourseTitle ? list[i].microCourseTitle : ''
    let hasFile = list[i].hasFile ? list[i].hasFile : ''
    let fileTitle = list[i].fileTitle ? list[i].fileTitle : ''
    let fileType = list[i].fileType ? list[i].fileType : ''
    let pay = list[i].pay ? list[i].pay : ''
    let memberId = list[i].memberId ? list[i].memberId : ''
    let custom1 = list[i].custom1 ? list[i].custom1 : ''
    let custom2 = list[i].custom2 ? list[i].custom2 : ''
    let custom3 = list[i].custom3 ? list[i].custom3 : ''
    let createdAt = list[i].createdAt ? list[i].createdAt : ''

    tmp.push(name)
    tmp.push(email)
    tmp.push(mobile)
    tmp.push(title)
    tmp.push(gender)
    tmp.push(position)
    tmp.push(subject)
    tmp.push(course1)
    tmp.push(course2)
    tmp.push(fax)
    tmp.push(telephone)
    tmp.push(organization)
    tmp.push(address)
    tmp.push(postCode)
    tmp.push(department)
    tmp.push(className)
    tmp.push(hotel)
    tmp.push(startDate)
    tmp.push(endDate)
    tmp.push(qq)
    tmp.push(suggest)
    tmp.push(startVehicleNo)
    tmp.push(endVehicleNo)
    tmp.push(role)
    tmp.push(invoiceTitle)
    tmp.push(invoiceContent)
    tmp.push(invoiceBrief)
    tmp.push(studyMode)
    tmp.push(hasMicroCourse)
    tmp.push(microCourseTitle)
    tmp.push(hasFile)
    tmp.push(fileTitle)
    tmp.push(fileType)
    tmp.push(pay)
    tmp.push(memberId)
    tmp.push(custom1)
    tmp.push(custom2)
    tmp.push(custom3)
    tmp.push(tool.formatYYYYMMDD(createdAt))
    earray1.push(tmp)
  }
  conf1.rows = earray1
  let cn = [conf1]
  let result = excelexport.execute(cn)
  await fs.writeFileSync(dir + '/' + randnum, result, 'binary')
  res.json({
    callBack: '0',
    url: '/admin/tmp/' + randnum
  })
}

exports.exportConferenceApplyFile = async (req, res, next) => {
  let randnum = tool.getOutTradeNo()
  let dir = 'assets/tmp/'
  // if (!fs.existsSync(dir_folder)) fs.mkdirSync(dir_folder)

  let conference = req.query.conference ? req.query.conference : '#'

  let list = await ConferenceApplyFile.find({ conference: conference, status: config.status.normal })
    .populate('attachment')
    .exec()

  let output = fs.createWriteStream(dir + randnum + '.zip')
  let archive = archiver('zip', {
    store: true
  })
  archive.pipe(output)

  for (let i in list) {
    let name = list[i].name ? list[i].name : ''
    let email = list[i].email ? list[i].email : ''
    let fileTitle = list[i].fileTitle ? list[i].fileTitle : ''
    let attachment = list[i].attachment
    console.log(attachment)
    let fileName = name + '_' + list[i]._id + '_' + attachment.origName
    // await request(attachment.downloadUrl).pipe(fs.createWriteStream(dir_folder + fileName))
    await archive.append(request(attachment.downloadUrl), { name: '/' + fileName })
  }

  await archive.finalize()

  res.json({
    callBack: '0',
    url: '/admin/tmp/' + randnum + '.zip'
  })
}

exports.normalBookManage = async function(req, res, next) {
  res.renderPjax('admin/book/manage_normal_book', {
    status: req.query.status ? req.query.status : 'normal'
  })
}

exports.preBookManage = async function(req, res, next) {
  res.renderPjax('admin/book/manage_pre_book', {
    status: req.query.status ? req.query.status : 'normal'
  })
}

exports.updateBookStatus = async function(req, res, next) {
  try {
    await Book.findByIdAndUpdate(req.body._id, { status: req.body.status })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1', msg: err })
  }
}

exports.updateBookCommend = async function(req, res, next) {
  try {
    if (req.body.type == 'key') await Book.findByIdAndUpdate(req.body._id, { isKeyBook: req.body.status })
    else await Book.findByIdAndUpdate(req.body._id, { isNewBook: req.body.status })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.chiefCommendManage = async function(req, res, next) {
  let token = qn.uptoken('node2d-public')
  res.renderPjax('admin/other/manage_chiefCommend', {
    status: req.query.status ? req.query.status : 'normal',
    token: token
  })
}

exports.addChiefCommend = async function(req, res, next) {
  try {
    let chiefCommend = new ChiefCommend(req.body.chiefCommend)
    chiefCommend._id = shortid.generate()
    chiefCommend.status = config.status.normal
    chiefCommend.creator = req.user
    await chiefCommend.save()

    res.redirect('/admin/chiefCommendManage')
  } catch (err) {
    console.log(err)
  }
}

exports.editChiefCommend = async function(req, res, next) {
  try {
    let chiefCommend = new ChiefCommend()
    if (req.body.chiefCommend_id != '') {
      chiefCommend = await ChiefCommend.findById(req.body.chiefCommend_id)
      if (!chiefCommend) {
        chiefCommend._id = shortid.generate()
      }
    } else {
      chiefCommend._id = shortid.generate()
    }
    chiefCommend = _.assignIn(chiefCommend, req.body.chiefCommend)
    let result = await uploadFileToQn(req, chiefCommend.cover)

    if (result.status == 'success') {
      chiefCommend.cover = result.fileId
    }
    await chiefCommend.save()

    res.redirect('/admin/chiefCommendManage?status=' + chiefCommend.status)
  } catch (err) {
    console.log(err)
  }
}

exports.uploadChiefCommendCover = async function(req, res, next) {
  try {
    let chiefCommend = await ChiefCommend.findById(req.body.chiefCommend_id).exec()
    let result = await uploadFileToAliyun(config.oss_url.chiefCommend_cover, req)
    if (result.status == 'success') {
      chiefCommend.cover = result.fileId
      await chiefCommend.save()
    } else {
      req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '上传失败。')
  }
  res.redirect('/admin/chiefCommendManage?status=' + req.body.chiefCommend_status)
}

exports.updateChiefCommendStatus = async function(req, res, next) {
  try {
    if (req.body.status == 'published') await ChiefCommend.findByIdAndUpdate(req.body._id, { status: req.body.status, publishDate: tool.now() })
    else await ChiefCommend.findByIdAndUpdate(req.body._id, { status: req.body.status })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.guanggaoManage = async function(req, res, next) {
  res.renderPjax('admin/other/manage_guanggao', {
    status: req.query.status ? req.query.status : 'normal'
  })
}

exports.saveGuanggao = async function(req, res, next) {
  try {
    let guanggao = new Guanggao()
    if (req.body._id) {
      guanggao = await Guanggao.findById(req.body._id)
      if (!guanggao) {
        guanggao = new Guanggao()
        guanggao._id = shortid.generate()
      }
    } else {
      guanggao._id = shortid.generate()
    }
    guanggao = _.assignIn(guanggao, req.body)
    let result = await uploadFileToQn(req, guanggao.cover)

    console.log(result)
    if (result.status == 'success') {
      guanggao.cover = result.fileId
    }
    await guanggao.save()

    res.redirect('/admin/guanggaoManage?status=' + guanggao.status)
  } catch (err) {
    console.log(err)
  }
}

exports.updateGuanggaoStatus = async function(req, res, next) {
  try {
    await Guanggao.findByIdAndUpdate(req.body._id, { status: req.body.status })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.login = async (req, res, next) => {
  // 处理sso登录

  let redirectUrl = req.query.redirectUrl ? req.query.redirectUrl : req.cookies.redirectUrl ? req.cookies.redirectUrl : 'https://new.hep.com.cn'

  redirectUrl = 'https://sso.hep.com.cn/login?accessKey=000bb26c14ab11e989d&targetUrl=' + utility.base64encode(redirectUrl)

  return res.redirect(redirectUrl)
}

exports.logoutSso = async (req, res, next) => {
  let token = req.body.token
  if (!token) {
    token = req.cookies['sso-token']
  }
  if (token) {
    let resp = await sso.ssoLogout(token)
    log.info(resp, 'logout resp')
    res.clearCookie('sso-token')
  }
  if (req.session) {
    // delete session object
    req.session.destroy(function(err) {
      if (err) {
        log.error(err)
      }
    })
  }

  res.clearCookie(config.token_name)
  res.clearCookie('redirectUrl')
  let redirectUrl = req.query.redirectUrl ? req.query.redirectUrl : '/admin'
  log.info('logout ok ...continue')
  return res.redirect(redirectUrl)
}

exports.keyProjectManage = async function(req, res, next) {
  res.renderPjax('admin/other/manage_keyProject')
}

exports.addKeyProjectDo = async function(req, res, next) {
  try {
    let keyProject = new KeyProject(req.body.keyProject)
    keyProject._id = shortid.generate()
    keyProject.status = config.status.published
    await keyProject.save()

    res.redirect('/admin/keyProjectManage')
  } catch (err) {
    console.log(err)
  }
}

exports.editKeyProjectDo = async function(req, res, next) {
  try {
    let keyProject = new KeyProject(req.body.keyProject)
    await KeyProject.findOneAndUpdate({ _id: keyProject._id }, keyProject, { up: 'up' })

    res.redirect('/admin/keyProjectManage')
  } catch (err) {
    console.log(err)
  }
}

exports.deleteKeyProject = async function(req, res, next) {
  try {
    await KeyProject.findByIdAndUpdate(req.body._id, { status: config.status.delete })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.deleteFile = async function(req, res, next) {
  try {
    await FileMeta.findByIdAndUpdate(req.body._id, { status: config.status.delete })
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.addConferencePhotoDo = async function(req, res, next) {
  let conference = await Conference.findById(req.body.conference_id).exec()
  try {
    let result = await uploadFileToAliyun(config.oss_url.conference_photo, req)
    if (result.status == 'success') {
      let photos = conference.photos
      photos.push.apply(photos, [result.fileId])
      conference.photos = photos
      await conference.save()
    } else {
      req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '上传失败。')
  }
  res.redirect('/admin/conferencePhoto?id=' + conference._id)
}

exports.deleteConferencePhoto = async function(req, res, next) {
  try {
    await Conference.findOneAndUpdate(
      {
        _id: req.body.conference_id
      },
      {
        $pull: {
          photos: req.body._id
        }
      },
      {
        up: 'up'
      }
    )
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.deleteConferenceNews = async function(req, res, next) {
  try {
    await News.findByIdAndUpdate(req.body._id, { status: config.status.delete })
    await Conference.findOneAndUpdate(
      {
        _id: req.body.conference_id
      },
      {
        $pull: {
          notice: req.body._id
        }
      },
      {
        up: 'up'
      }
    )
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}

exports.updateRealNameStatus = async function(req, res, next) {
  try {
    let message = ''
    let status
    if (req.body.status == 'published') {
      message = '您好！恭喜您已经通过实名教师认证审核，可在教师服务板块中下载相关资源。感谢您一直以来对高等教育出版社的关注与信任！'
      status = '1'
    } else {
      message =
        '您好！您提交的实名教师认证申请由于' + req.body.reason + '的原因没有通过我们的验证，请您登录www.hep.com.cn/service/realname，进行资料补充。'
      status = '2'
    }
    emailTool.sendRealTeacher(['173681289@qq.com'], '谢涛', '高等教育出版社实名教师认证', message)

    let teacher = await RealNameTeacher.findByIdAndUpdate(req.body._id, { status: req.body.status, checker: req.user, reason: req.body.reason })

    // console.log(teacher.username)
    let user = await User.findOne({ username: teacher.username })

    let authenticationStatus = '0'
    let roles = user.roles
    let index = roles.indexOf(config.role.teacher)
    if (req.body.status === 'reject') {
      authenticationStatus = '2'
      if (index != -1) {
        roles.splice(index, 1)
      }
    }
    if (req.body.status === 'published') {
      authenticationStatus = '1'
      if (index == -1) {
        roles.push(config.role.teacher)
      }
    }
    user.roles = roles
    await user.save()

    let userIdentityInfo = {
      userId: teacher.sso_id,
      authenticationStatus: authenticationStatus
    }
    let token = req.cookies['sso-token']
    let result = await sso.updateTeacherInfo(token, userIdentityInfo)
    log.info(result, 'sso ', token)

    res.send({ callBack: '0' })
  } catch (err) {
    console.log(err)
    res.send({ callBack: '1' })
  }
}

exports.slideManage = async function(req, res, next) {
  let page = req.query.page ? req.query.page : 1
  let showPlace = req.query.showPlace ? req.query.showPlace : 'index'
  var options = {
    classNameSpace: '',
    perPage: 20,
    page: page,
    firstText: '首页',
    lastText: '末页',
    totalText: '%d / %d 共 %d 条',
    query: { showPlace: showPlace, status: { $ne: config.status.delete } }
  }

  var query = Slide.find({ showPlace: showPlace, status: { $ne: config.status.delete } }).populate('cover')

  query.paginater(options, function(err, result) {
    console.log(result)
    res.renderPjax('admin/other/manage_slide', {
      results: result,
      showPlace: showPlace
    })
  })
}

exports.addSlide = async function(req, res, next) {
  let slide
  try {
    slide = new Slide(req.body.slide)
    slide._id = shortid.generate()
    slide.status = config.status.checked
    slide.creator = req.user

    let result = await uploadFileToAliyun(config.oss_url.slide_cover, req)
    if (result.status == 'success') {
      slide.cover = result.fileId
      await slide.save()
    } else {
      req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '操作失败。')
  }
  res.redirect('/admin/slideManage?showPlace=' + slide.showPlace)
}

exports.editSlide = async function(req, res, next) {
  let slide = req.body.slide
  try {
    let result = await uploadFileToAliyun(config.oss_url.slide_cover, req)
    if (result.status == 'success') {
      slide.cover = result.fileId
      slide = await Slide.findOneAndUpdate({ _id: slide._id }, slide, { up: 'up' })
    } else {
      slide = await Slide.findOneAndUpdate({ _id: slide._id }, slide, { up: 'up' })
      // req.flash('notice', '操作失败。')
    }
  } catch (err) {
    console.log(err)
    req.flash('notice', '操作失败。')
  }
  console.log(slide)
  res.redirect('/admin/slideManage?showPlace=' + slide.showPlace)
}

exports.commendToSlide = async function(req, res, next) {
  try {
    let news = await News.findById(req.body._id)
    let slide = new Slide()
    slide._id = shortid.generate()
    slide.title = news.title
    slide.url = '/news/show/' + news.alias
    slide.brief = news.title
    slide.showPlace = 'index'
    slide.status = config.status.checked
    slide.creator = news.creator
    slide.cover = news.cover
    slide.save()
    res.send({ callBack: '0' })
  } catch (err) {
    res.send({ callBack: '1' })
  }
}
