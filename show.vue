<template>
  <div id="layout">
    <Header></Header>
    <Menu></Menu>
    <el-container>
      <el-main style="padding:0">
        <div class="new_wrapper bggray">
          <div class="new_container padtb30">
            <!--面包屑-->
            <el-row class="marb20">
              <el-col :span="24">
                <el-breadcrumb separator-class="el-icon-arrow-right ">
                  <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
                  <el-breadcrumb-item :to="{ path: '/news/' }">高教资讯</el-breadcrumb-item>
                  <el-breadcrumb-item>资讯详情</el-breadcrumb-item>
                </el-breadcrumb>
              </el-col>
            </el-row>
            <el-row class="marb40">
              <el-row>
                <el-col :span="24" class="bgwhite bdround padtb40">
                  <div class="main_article padlr200">
                    <div class="article_title">
                      <h2 class="blue2">{{news.title}}</h2>
                      <!-- 副标题 需调整样式 -->
                      <h3 v-if="news.subTitle" class="black2 lh-40 f_18 f_right">——{{news.subTitle}}</h3>
                      <a href="#d_attach">
                        <el-button
                          type="primary f_right"
                          style="margin-top:30px;"
                          v-if="news.attachments!=''"
                        >下载附件</el-button>
                      </a>
                      <ul class="newstime">
                        <li>时间：{{news.publishDate}}</li>
                        <li>来源：{{news.from}}</li>
                      </ul>
                      <hr class="marb20" style="border-top:1px solid #959595" />

                      <div v-html="news.content"></div>
                    </div>
                  </div>
                  <div style="padding:0 80px;" v-if="news.attachments!=''">
                    <div class="d_attach" style="min-height:150px;" id="d_attach">
                      <div class="d_title">
                        <ul v-for="(a,index) in news.attachments" :key="a._id">
                          <li>
                            <el-row>
                              <el-col
                                :span="20"
                                style="overflow: hidden;text-overflow:ellipsis;white-space: nowrap;"
                              >
                                <a
                                  :href="a.downloadUrl"
                                  :title="a.origName"
                                  :download="a.origName"
                                >附件{{index+1}}：{{a.origName}}</a>
                              </el-col>
                              <el-col :span="4">
                                <span>{{bytesToSize(a.origSize?a.origSize:0)}}</span>
                              </el-col>
                            </el-row>
                          </li>
                        </ul>
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </el-row>
          </div>
        </div>
      </el-main>
    </el-container>
    <Footer></Footer>
  </div>
</template>

<script>
import { getNewsByAlias } from '../../api/news.js'
import Footer from '../../components/Footer'
import Header from '../../components/Header'
import Menu from '../../components/Menu'
import Contact from '../../components/Contact'
import Udesk from '../../components/Udesk'
export default {
  data() {
    return {
      key: '',
      news: {}
    }
  },
  name: 'app',
  components: { Header, Menu, Footer, Contact, Udesk },
  created() {
    getNewsByAlias(this.$route.params.alias).then(data => {
      console.log(data)
      if (data.success === true && data.total > 0) {
        this.news = data.rows[0]
        document.title = this.news.title + '|' + this.$route.meta.title
      } else {
        // window.location.href = '/404'
      }
    })
  },
  computed: {
    bytesToSize: function() {
      return function(bytes) {
        if (bytes === 0) return '0 B'
        let k = 1024
        let sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
        let i = Math.floor(Math.log(bytes) / Math.log(k))
        return parseInt(bytes / Math.pow(k, i)) + ' ' + sizes[i]
      }
    }
  }
}
</script>
<style scoped>
@import url('../../assets/news.css');
</style>
