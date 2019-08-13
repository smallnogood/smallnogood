package com.hep.zjk;


import com.hep.zjk.util.ScaleImage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


public class HepBook {


    static Logger log = Logger.getLogger(HepBook.class.getName());

    private final static String baseURL = "http://47.93.239.69";

    private final static String POSTURL = baseURL + "/xpe/hep/service/sync/getPostData";
    private final static String POSTURL1 = baseURL + "/xpe/hep/service/sync/getPostData1";

    private final static String baseFile = "/tmp";

    private Connection hepmdConn;

    private Connection hepoutConn;


    public static void main(String[] args) {
        log.info("sync begin");
        HepBook a = new HepBook();
        try{
            a.syncData();
//            a.updateAllFile1();
//            a.updateByWuliaos1();
        } catch (Exception e) {
            e.printStackTrace();
            log.info("main error "+e);
        }
        log.info("sync end");

    }


    @Test
    public void updateByWuliaos() throws Exception {


        List<String> wuliaos = FileUtils.readLines(new File("/tmp/wuliao.txt"));
        int max = 30000;
        int i=0;
        for (String wuliao : wuliaos) {

            try {
                PortalBook book = getBook(wuliao);

                if (book != null) {

                    log.info("do book" + book.getBookID());

                    System.out.println(i++);
                    String cmsId=CMSUtil.getCMSIdByWuliao(book.getBookID());
                    book.setCmsId(cmsId);
//                    if("".equals(cmsId))continue;

                    boolean coverFlag=false;
                    boolean introFlag=false;
                    boolean tocFlag=false;
                    boolean pdfFlag=false;
                    boolean abookCoverFlag=false;
                    File fileDir=null;
                    coverFlag=prepareCover(book);
                    introFlag=prepareIntroduce(book);
                    tocFlag=prepareToc(book);
//                    pdfFlag=preparePdf(book);
//                    abookCoverFlag=prepareAbookCover(book);

//                    if(!coverFlag)continue;

                    if(coverFlag||introFlag||pdfFlag||tocFlag||abookCoverFlag) {
                        String zip = baseFile + "/" + book.getBookID() + ".zip";
                        ZipUtil.zipFiles(baseFile + "/" + book.getBookID());
                        book.setZip(zip);
                    }else{
                        continue;
                    }

                    book.setCmsId(CMSUtil.getCMSIdByWuliao(book.getBookID()));
//                    book.setErpBrief(CMSUtil.getCommend(cmsId));
//                    book.setTerm(CMSUtil.getTermByWuliao(book.getBookID()));
//                    book.setSubTitle(CMSUtil.getSubTitle(cmsId));
//                    book.setAuthorBrief(CMSUtil.getAuthorBrief(cmsId));
//                    book.setSeriesName(CMSUtil.getSeriesByWuliao(book.getBookID()));
                    Boolean is2d=CMSUtil.get2Dbook(book.getBookID());
//                    Boolean isEbook=CMSUtil.getMallByWuliao(book.getBookID());
//                    String abook=CMSUtil.getAbook(cmsId);
//                    if(isEbook)book.setIsEbook("yes");
                    if(is2d){
                        book.setIsAbook("yes");
                    }else{
                        book.setIsAbook("no");
                    }
//                    if(!"".equals(abook)){
//                        book.setIsAbookCourse("yes");
//                        book.setAbookURL(abook);
//                        book.setAbookBrief(CMSUtil.getAbookBrief(cmsId));
//                    }else{
//                        book.setIsAbookCourse("no");
//                    }

                    doPost(book);
                    fileDir = new File(baseFile + "/" + book.getBookID());
                    if (fileDir.exists()) {
                        deleteDir(fileDir);
                    }
                    fileDir = new File(baseFile + "/" + book.getBookID() + ".zip");
                    if (fileDir.exists()) {
                        fileDir.delete();
                    }
                } else {
                    log.error("book not exist" + wuliao);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            max--;
            if (max < 0) break;

        }
    }



    @Test
    public void updateAllFile() throws Exception {

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR,-9);
        String nowTime=(new java.text.SimpleDateFormat("yyyyMMddHHmmss")).format(c.getTime());
        System.out.println(nowTime);
        log.info("time= "+nowTime);

        List<String> resIdList = CMSUtil.getCMSIdsByUpdated(nowTime);
        List<String> wbss = new ArrayList<String>();
        System.out.println(resIdList.size());
        String wbs=null;
        for(int i=0;i<resIdList.size();i++){
            wbs=getWbsByCmsId(resIdList.get(i));
//            System.out.println(wbs);
            if(null!=wbs && !"".equals(wbs)){
                wbss.add(wbs);
            }
        }

        List<PortalBook> list = getBooks(wbss);

        boolean coverFlag=false;
        boolean introFlag=false;
        boolean tocFlag=false;
        boolean pdfFlag=false;
        File fileDir=null;

        int i=0;
        for (PortalBook book : list) {

            log.info("do book " + book.getBookID() + book.getTitle());

            System.out.println(i++);
            try {
                coverFlag=prepareCover(book);
                introFlag=prepareIntroduce(book);
                tocFlag=prepareToc(book);
                pdfFlag=preparePdf(book);
                book.setErpBrief(CMSUtil.getCommend(CMSUtil.getCMSIdByWuliao(book.getBookID())));
                book.setTerm(CMSUtil.getTermByWuliao(book.getBookID()));
                book.setSeriesName(CMSUtil.getSeriesByWuliao(book.getBookID()));
                Boolean is2d=CMSUtil.get2Dbook(book.getBookID());
//                Boolean isEbook=CMSUtil.getEbookByWuliao(book.getBookID());
                String abook=CMSUtil.getAbook(CMSUtil.getCMSIdByWuliao(book.getBookID()));
                if(is2d)book.setIsAbook("yes");
//                if(isEbook)book.setIsEbook("yes");
                if(!"".equals(abook)){
                    book.setIsAbookCourse("yes");
                    book.setAbookURL(abook);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.info("error" + book.getBookID()+" "+e);
                continue;
            }

            if(coverFlag||introFlag||pdfFlag||tocFlag) {
                String zip = baseFile + "/" + book.getBookID() + ".zip";
                ZipUtil.zipFiles(baseFile + "/" + book.getBookID());
                book.setZip(zip);
            }
            doPost(book);
            fileDir = new File(baseFile + "/" + book.getBookID());
            if (fileDir.exists()) {
                deleteDir(fileDir);
            }
            fileDir = new File(baseFile + "/" + book.getBookID() + ".zip");
            if (fileDir.exists()) {
                fileDir.delete();
            }

        }
    }

    @Test
    public void updateByWuliaos1() throws Exception {


        List<String> cmsIds = FileUtils.readLines(new File("/tmp/wuliao.txt"));
        boolean coverFlag=false;
        boolean introFlag=false;
        boolean tocFlag=false;
        boolean pdfFlag=false;
        List<String> tmpList=null;
        File fileDir=null;
        int i=0;
        PortalBook book = null;
        for (String cmsId : cmsIds) {
            System.out.println(i++);

            try {
                book=new PortalBook();
                book.setCmsId(cmsId);
                tmpList=CMSUtil.getWbsById(cmsId);
                book.setWbs(tmpList.get(0));
                book.setBookID(tmpList.get(1));


                if (book != null) {
                    //
                    log.info("do book" + book.getCmsId());


                    coverFlag=prepareCover1(book);
                    introFlag=prepareIntroduce1(book);
                    tocFlag=prepareToc1(book);
//                    pdfFlag=preparePdf1(book);

                    if(coverFlag||introFlag||pdfFlag||tocFlag) {
                        String zip = baseFile + "/" + cmsId + ".zip";
                        ZipUtil.zipFiles(baseFile + "/" + cmsId);
                        book.setZip(zip);
                    }else{
                        continue;
                    }
//                    Boolean is2d=CMSUtil.get2Dbook(book.getBookID());
                    Boolean isEbook=CMSUtil.getMallByWuliao(book.getBookID());
//                    String abook=CMSUtil.getAbook(cmsId);
                    if(isEbook)book.setIsEbook("yes");
//                    if(is2d){
//                        book.setIsAbook("yes");
//                    }else{
//                        book.setIsAbook("no");
//                        System.out.println("11111");
//                    }
//                    book.setErpBrief(CMSUtil.getCommend(book.getCmsId()));
//                    String abook=CMSUtil.getAbook(book.getCmsId());
//                    if(!"".equals(abook)){
//                        book.setIsAbookCourse("yes");
//                        book.setAbookURL(abook);
//                    }

                    doPost1(book);
                    fileDir = new File(baseFile + "/" + cmsId);
                    if (fileDir.exists()) {
                        deleteDir(fileDir);
                    }
                    fileDir = new File(baseFile + "/" + cmsId + ".zip");
                    if (fileDir.exists()) {
                        fileDir.delete();
                    }
                } else {
                    log.error("book not exist" + cmsId);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Test
    public void updateAllFile1() throws Exception {

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR,-9);
        String nowTime=(new java.text.SimpleDateFormat("yyyyMMddHHmmss")).format(c.getTime());
        System.out.println(nowTime);
        log.info("time= "+nowTime);

        List<String> resIdList = CMSUtil.getCMSIdsByUpdated(nowTime);
//        List<String> resIdList = CMSUtil.getCMSIdsByUpdated("20190102050004");
        List<String> wbss = new ArrayList<String>();
        List<String> wuliaos = new ArrayList<String>();
        String wbs=null;
        String cmsId=null;
        String wuliao=null;
        List<String> tmpList=null;
        for(int i=0;i<resIdList.size();i++){
            tmpList=CMSUtil.getWbsById(resIdList.get(i));
            wbss.add(tmpList.get(0));
            wuliaos.add(tmpList.get(1));
        }

        System.out.println(resIdList.size());

        boolean coverFlag=false;
        boolean introFlag=false;
        boolean tocFlag=false;
        boolean pdfFlag=false;
        boolean abookCoverFlag=false;
        File fileDir=null;
        PortalBook book = null;

        int j=0;
        for (int i=0;i<resIdList.size();i++) {

            cmsId=resIdList.get(i);
            wbs=wbss.get(i);
            wuliao=wuliaos.get(i);
            log.info("do book wuliao=" + wuliao+" wbs="+wbs);
            if(null==wbs || "".equals(wbs)){
                log.info("wbs is null");
                continue;
            }

            book=new PortalBook();
            book.setCmsId(cmsId);
            book.setWbs(wbs);
            book.setBookID(wuliao);


            System.out.println(j++);
            try {
                coverFlag=prepareCover(book);
                introFlag=prepareIntroduce(book);
                tocFlag=prepareToc(book);
//                pdfFlag=preparePdf(book);
                abookCoverFlag=prepareAbookCover(book);

                book.setErpBrief(CMSUtil.getCommend(cmsId));
                book.setAuthorBrief(CMSUtil.getAuthorBrief(cmsId));
                book.setSubTitle(CMSUtil.getSubTitle(cmsId));
                book.setTerm(CMSUtil.getTermByWuliao(wuliao));
                book.setSeriesName(CMSUtil.getSeriesByWuliao(wuliao));
                Boolean is2d=CMSUtil.get2Dbook(wuliao);
                Boolean isEbook=CMSUtil.getMallByWuliao(wuliao);
                String abook=CMSUtil.getAbook(cmsId);
                if(is2d)book.setIsAbook("yes");
                if(isEbook)book.setIsEbook("yes");
                if(!"".equals(abook)){
                    book.setIsAbookCourse("yes");
                    book.setAbookURL(abook);
                    book.setAbookBrief(CMSUtil.getAbookBrief(cmsId));
                }

            } catch (Exception e) {
                e.printStackTrace();
                log.info("error" + wuliao+" "+e);
                continue;
            }


            if(coverFlag||introFlag||pdfFlag||tocFlag||abookCoverFlag) {
                String zip = baseFile + "/" + wuliao + ".zip";
                ZipUtil.zipFiles(baseFile + "/" + wuliao);
                book.setZip(zip);
            }
            doPost1(book);
            fileDir = new File(baseFile + "/" + wuliao);
            if (fileDir.exists()) {
                deleteDir(fileDir);
            }
            fileDir = new File(baseFile + "/" + wuliao + ".zip");
            if (fileDir.exists()) {
                fileDir.delete();
            }

        }
    }

//    @Test
//    public void updateAllFile1() throws Exception {
//
//        Calendar c = Calendar.getInstance();
//        c.setTime(new Date());
//        c.add(Calendar.HOUR,-9);
//        String nowTime=(new java.text.SimpleDateFormat("yyyyMMddHHmmss")).format(c.getTime());
//        System.out.println(nowTime);
//
//        List<String> resIdList = CMSUtil.getCMSIdsByUpdated("20150722120000");
////        List<String> resIdList = CMSUtil.getCMSIdsByUpdated(nowTime);
//        List<String> wbss = new ArrayList<String>();
//        List<String> wuliaos = new ArrayList<String>();
//        String wbs=null;
//        String cmsId=null;
//        String wuliao=null;
//        List<String> tmpList=null;
//        for(int i=0;i<resIdList.size();i++){
//            tmpList=CMSUtil.getWbsById(resIdList.get(i));
//            wbss.add(tmpList.get(0));
//            wuliaos.add(tmpList.get(1));
//        }
//        System.out.println(resIdList.size());
//
//        boolean coverFlag=false;
//        boolean introFlag=false;
//        boolean tocFlag=false;
//        boolean pdfFlag=false;
//        File fileDir=null;
//        PortalBook book = null;
//
//        int j=0;
//        for (int i=0;i<resIdList.size();i++) {
//
//            cmsId=resIdList.get(i);
//            wbs=wbss.get(i);
//            wuliao=wuliaos.get(i);
//            log.info("do book wuliao=" + wuliao+" wbs="+wbs);
//            if(null==wbs || "".equals(wbs)){
//                log.info("wbs is null");
//                continue;
//            }
//
//            book=new PortalBook();
//            book.setCmsId(cmsId);
//            book.setWbs(wbs);
//            System.out.println(j++);
//            System.out.println(wbs);
//
//            try {
//                coverFlag=prepareCover1(book);
//                introFlag=prepareIntroduce1(book);
//                tocFlag=prepareToc1(book);
//                pdfFlag=preparePdf1(book);
//
//            } catch (Exception e) {
//                log.info("error" + wuliao+" "+e);
//                e.printStackTrace();
//                continue;
//            }
//
//            if(coverFlag||introFlag||pdfFlag||tocFlag) {
//                String zip = baseFile + "/" + cmsId + ".zip";
//                ZipUtil.zipFiles(baseFile + "/" + cmsId);
//                book.setZip(zip);
//            }
//            doPost1(book);
//            fileDir = new File(baseFile + "/" + cmsId);
//            if (fileDir.exists()) {
//                deleteDir(fileDir);
//            }
//            fileDir = new File(baseFile + "/" + cmsId + ".zip");
//            if (fileDir.exists()) {
//                fileDir.delete();
//            }
//
//        }
//    }

    @Test
    public void syncData() throws Exception {

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR,-9);
        String nowTime=(new java.text.SimpleDateFormat("yyyyMMddHHmmss")).format(c.getTime());
        System.out.println(nowTime);

        String sql = "select wbs from t_erp_data2  where to_char(updated, 'yyyymmddhh24miss')>?";
//        String sql = "select wbs from t_erp_data2  where wbs='14-21056-002'";
        PreparedStatement ps = getHepmdConn().prepareStatement(sql);
        ps.setString(1, nowTime);
//        ps.setString(1, "20190102040004");

        int max = 10000;

        ResultSet rs = ps.executeQuery();

        List<String> wbss = new ArrayList<String>();

        while (rs.next()) {
            String wbs = rs.getString("wbs");
            wbss.add(wbs);
//            log.info("add book to list " + wbs);
            max--;
            if (max < 0) break;
        }
        rs.close();
        ps.close();

        System.out.println("total="+wbss.size());


        List<PortalBook> list = getBooks(wbss);

        int i=0;
        for (PortalBook book : list) {
            System.out.println(i++);

            //
            log.info("do book " + book.getWbs() + book.getTitle());
//            boolean coverFlag=false;
//            boolean introFlag=false;
//            boolean tocFlag=false;
//            boolean pdfFlag=false;
//            coverFlag=prepareCover(book);
//                    introFlag=prepareIntroduce(book);
//                    tocFlag=prepareToc(book);
//                    pdfFlag=preparePdf(book);
//
//            if(coverFlag||introFlag||pdfFlag||tocFlag) {
//                String zip = baseFile + "/" + book.getBookID() + ".zip";
//                ZipUtil.zipFiles(baseFile + "/" + book.getBookID());
//                book.setZip(zip);
//            }
//            if(StringUtils.isNotBlank(book.getBookID())) {
//                try {
//                    book.setCmsId(CMSUtil.getCMSIdByWuliao(book.getBookID()));
//                    book.setSeriesName(CMSUtil.getSeriesByWuliao(book.getBookID()));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    continue;
//                }
//
//            }
            doPost(book);

        }
        System.out.println("success");
    }


    //数据库中 找到cover 然后下载下来 然后
    public boolean prepareCover(PortalBook book) throws Exception {

        String cover = CMSUtil.getCoverPath(book.getCmsId());
//        System.out.println(cover);

        if (StringUtils.isNotBlank(cover)&&(cover.contains(".jpg")||cover.contains(".JPG")||cover.contains(".png")||cover.contains(".PNG"))) {

            //wget
            File dst = new File(baseFile + "/" + book.getBookID()+ "/cover/" + book.getBookID() + "_front.jpg");

            try {
                FileUtils.copyURLToFile(new URL(cover), dst);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

//            图片压缩
            try {
                ScaleImage si=new ScaleImage();
                si.saveImageAsJpg(dst.getPath(), dst.getPath(), 180, 240);
            } catch (Exception e) {
                e.printStackTrace();
//                return false;
            }

            //打包

//            String zip = baseFile + "/" + book.getBookID() + "/" + book.getBookID() + ".zip";
//            ZipUtil.zip(zip, dst.getPath().replace("\\","/"));

            book.setCover(book.getBookID() + "/cover/" + book.getBookID() + "_front.jpg");
            return true;
        }
        return false;

    }

    public boolean prepareIntroduce(PortalBook book) throws Exception {

        String intro = CMSUtil.getBrief(book.getCmsId());

        if (StringUtils.isNotBlank(intro)) {

            File tocFile = new File(baseFile + "/" + book.getBookID() + "/introduce/index.html");
            FileUtils.writeStringToFile(tocFile, intro.replaceAll("\\\\\"","\"").replaceAll("\\\\r","\r").replaceAll("\\\\n","\n").replaceAll("\\\\t","\t"), "UTF-8", true);

            book.setIntroduce(book.getBookID() + "/introduce/index.html");

            return true;

        }else if(!book.getBookID().contains("-00")){
            String bookID0x=book.getBookID();
            String bookID00=bookID0x.substring(0,bookID0x.indexOf("-"))+"-00";
            intro = CMSUtil.getBrief(CMSUtil.getCMSIdByWuliao(bookID00));

            if (StringUtils.isNotBlank(intro)) {

                File tocFile = new File(baseFile + "/" + book.getBookID() + "/introduce/index.html");
                FileUtils.writeStringToFile(tocFile, intro.replaceAll("\\\\\"","\"").replaceAll("\\\\r","\r").replaceAll("\\\\n","\n").replaceAll("\\\\t","\t"), "UTF-8", true);

                book.setIntroduce(book.getBookID() + "/introduce/index.html");

                return true;

            }
            return false;
        }
        return false;

    }


    public boolean prepareToc(PortalBook book) throws Exception {

        String toc = CMSUtil.getToc(book.getCmsId());

        if (StringUtils.isNotBlank(toc)) {

            File tocFile = new File(baseFile + "/" + book.getBookID() + "/toc/index.html");
            FileUtils.writeStringToFile(tocFile, toc, "UTF-8", true);

            book.setToc(book.getBookID() + "/toc/index.html");

            return true;

        }else if(!book.getBookID().contains("-00")){
            String bookID0x=book.getBookID();
            String bookID00=bookID0x.substring(0,bookID0x.indexOf("-"))+"-00";
            toc = CMSUtil.getToc(CMSUtil.getCMSIdByWuliao(bookID00));

            if (StringUtils.isNotBlank(toc)) {

                File tocFile = new File(baseFile + "/" + book.getBookID() + "/toc/index.html");
                FileUtils.writeStringToFile(tocFile, toc, "UTF-8", true);

                book.setToc(book.getBookID() + "/toc/index.html");

                return true;

            }
            return false;
        }
        return false;

    }

    public boolean preparePdf(PortalBook book) throws Exception {

        List<HashMap> pdfList = CMSUtil.getPdf(book.getCmsId());
        String pdfPath="";

        if (pdfList!=null&&pdfList.size()!=0) {
            for (HashMap<String,String> pdfMap : pdfList) {
                String name=pdfMap.get("name");
                String url=pdfMap.get("url");
                File dst = new File(baseFile + "/" + book.getBookID()+ "/pdf/" + name);

                try {
                    FileUtils.copyURLToFile(new URL(url), dst);
                    if(!"".equals(pdfPath))
                        pdfPath=pdfPath+",";
                    pdfPath=pdfPath+book.getBookID() + "/pdf/" + name;

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

            }

            book.setPdf(pdfPath);
            return true;

        }
        return false;

    }

    public boolean prepareAbookCover(PortalBook book) throws Exception {

        String cover = CMSUtil.getAbookCover(book.getCmsId());
//        System.out.println(cover);

        if (StringUtils.isNotBlank(cover)&&(cover.contains(".jpg")||cover.contains(".JPG")||cover.contains(".png")||cover.contains(".PNG"))) {

            //wget
            File dst = new File(baseFile + "/" + book.getBookID()+ "/cover/" + book.getBookID() + "_abook.jpg");

            try {
                FileUtils.copyURLToFile(new URL(cover), dst);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

//            图片压缩
//            try {
//                ScaleImage si=new ScaleImage();
//                si.saveImageAsJpg(dst.getPath(), dst.getPath(), 180, 240);
//            } catch (Exception e) {
//                e.printStackTrace();
////                return false;
//            }



            book.setAbookCoverPath(book.getBookID() + "/cover/" + book.getBookID() + "_abook.jpg");
            return true;
        }
        return false;

    }


    public List<PortalBook> getBooks(List<String> wbss) {
        List<PortalBook> list = new ArrayList<PortalBook>();
        if (wbss != null) {
            for (String wbs : wbss) {
                try {

                    String sql = "select * from t_erp_data2 where wbs=? order by updated desc";
                    PreparedStatement ps = getHepmdConn().prepareStatement(sql);
                    ps.setString(1, wbs);

                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        PortalBook b = new PortalBook();
                        b.setWbs(rs.getString("wbs"));
                        b.setBookID(rs.getString("wuliao"));
                        if (StringUtils.isNotBlank(b.getBookID())) {
                            String subid = StringUtils.substring(b.getBookID(), 0, b.getBookID().lastIndexOf("-"));
                            b.setSubBookID(subid);
                        }

                        b.setReader(rs.getString("reader_level"));
                        b.setKeyProject01(rs.getString("project_name"));
                        b.setKeyProject02(rs.getString("project_name2"));
                        b.setKeyProject03(rs.getString("project_name3"));
                        b.setCarrierType(rs.getString("vector_type"));
                        b.setPrice(rs.getString("price"));
                        String s = rs.getString("publish_time");
                        if (StringUtils.isNotBlank(s)) {
                            b.setPublishTime(rs.getString("publish_time").replace(".", "-"));
                        }
                        String p = rs.getString("yjcbsj");
                        if (StringUtils.isNotBlank(p) && p.length()==8) {
                            b.setPlanPublishTime(p.substring(0,4)+"-"+p.substring(4,6)+"-"+p.substring(6,8));
                        }

                        b.setFormat(rs.getString("booksize"));
                        b.setPageNum(rs.getString("pagecount"));
                        b.setWords(rs.getString("bmzs"));
                        b.setPrintColor(rs.getString("jsys"));
                        b.setBinding(rs.getString("zzys"));
                        b.setReferenceEditor(rs.getString("zrbj"));
                        b.setResourceType(rs.getString("cb"));
                        b.setCopyright(rs.getString("bqdjh"));
                        b.setVersion(rs.getString("bb"));
                        b.setVolumns(rs.getString("jcs"));
                        b.setLanguage(rs.getString("wz"));
                        b.setSeries(rs.getString("ctsm"));
                        b.setPublishStatus(rs.getString("cbzt"));
                        b.setEdition(rs.getString("bc"));
                        b.setCourseType_lk_display(rs.getString("kclx"));
                        b.setImpression(rs.getString("yc"));
                        b.setStudyURL(rs.getString("url4a"));
                        b.setAuthor(rs.getString("author"));
                        b.setTitle(rs.getString("title"));
                        b.setDepartmentID_lk_display(rs.getString("hep_department"));
                        b.setDepartmentID(rs.getString("hep_department_id"));
                        b.setErpSubject_lk_display(rs.getString("subject"));
                        s = rs.getString("new_print_time");

                        if (StringUtils.isNotBlank(s)) {
                            b.setPrintDate(s.replace(".", "-"));
                        }

                        b.setErpSubject(rs.getString("subject_id"));
                        b.setIsTrain(rs.getString("for_training"));
                        b.setIsNormal(rs.getString("for_normalschool"));
                        b.setIsAdult(rs.getString("for_adult"));
                        b.setIsExam(rs.getString("for_examination"));
                        b.setCategory(rs.getString("publication_type"));

                        b.setCategory_lk_display(rs.getString("publication_type_desc"));
                        b.setImportBook(rs.getString("imported_book"));
                        b.setSubject01(rs.getString("typical_subject_range_id"));
                        b.setSubject02(rs.getString("typical_subject_sub_range_id"));
                        b.setSubject03(rs.getString("typical_course_range_id"));
                        b.setSpecialty(rs.getString("typical_speciality_range"));
                        b.setSpecial_lk_display(rs.getString("typical_speciality_range_id"));
                        b.setDownStatus(rs.getString("status"));
                        b.setDataClean(rs.getString("clean"));
                        b.setIsbn(rs.getString("isbn"));
                        b.setResponsibleEditor(rs.getString("chbj"));
                        b.setCarrierNum(rs.getString("ztsl"));
                        b.setSourceCarrierBookID(rs.getString("kbwxx"));
                        b.setReplacedBookID(rs.getString("tdpzsh"));
                        b.setCourseType(rs.getString("kclx_id"));

                        b.setTempTitle(rs.getString("xtmc"));
                        b.setAssessPrice(rs.getString("gjdj"));
                        list.add(b);

                    }
                    rs.close();
                    ps.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return list;

    }


    public PortalBook getBook(String wbs) {


        try {

            String sql = "select * from t_erp_data2 where wuliao=? order by updated desc";
            PreparedStatement ps = getHepmdConn().prepareStatement(sql);
            ps.setString(1, wbs);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                PortalBook b = new PortalBook();

                b.setWbs(rs.getString("wbs"));
                b.setBookID(rs.getString("wuliao"));
                if (StringUtils.isNotBlank(b.getBookID())) {
                    String subid = StringUtils.substring(b.getBookID(), 0, b.getBookID().lastIndexOf("-"));
                    b.setSubBookID(subid);
                }

                b.setReader(rs.getString("reader_level"));
                b.setKeyProject01(rs.getString("project_name"));
                b.setKeyProject02(rs.getString("project_name2"));
                b.setKeyProject03(rs.getString("project_name3"));
                b.setCarrierType(rs.getString("vector_type"));
                b.setPrice(rs.getString("price"));
                String s = rs.getString("publish_time");
                if (StringUtils.isNotBlank(s)) {
                    b.setPublishTime(rs.getString("publish_time").replace(".", "-"));
                }
                String p = rs.getString("yjcbsj");
                if (StringUtils.isNotBlank(p) && p.length()==8) {
                    b.setPlanPublishTime(p.substring(0,4)+"-"+p.substring(4,6)+"-"+p.substring(6,8));
                }

                b.setFormat(rs.getString("booksize"));
                b.setPageNum(rs.getString("pagecount"));
                b.setWords(rs.getString("bmzs"));
                b.setPrintColor(rs.getString("jsys"));
                b.setBinding(rs.getString("zzys"));
                b.setReferenceEditor(rs.getString("zrbj"));
                b.setResourceType(rs.getString("cb"));
                b.setCopyright(rs.getString("bqdjh"));
                b.setVersion(rs.getString("bb"));
                b.setVolumns(rs.getString("jcs"));
                b.setLanguage(rs.getString("wz"));
                b.setSeries(rs.getString("ctsm"));
                b.setPublishStatus(rs.getString("cbzt"));
                b.setEdition(rs.getString("bc"));
                b.setCourseType_lk_display(rs.getString("kclx"));
                b.setImpression(rs.getString("yc"));
                b.setStudyURL(rs.getString("url4a"));
                b.setAuthor(rs.getString("author"));
                b.setTitle(rs.getString("title"));
                b.setDepartmentID_lk_display(rs.getString("hep_department"));
                b.setDepartmentID(rs.getString("hep_department_id"));
                b.setErpSubject_lk_display(rs.getString("subject"));
                s = rs.getString("new_print_time");

                if (StringUtils.isNotBlank(s)) {
                    b.setPrintDate(s.replace(".", "-"));
                }

                b.setErpSubject(rs.getString("subject_id"));
                b.setIsTrain(rs.getString("for_training"));
                b.setIsNormal(rs.getString("for_normalschool"));
                b.setIsAdult(rs.getString("for_adult"));
                b.setIsExam(rs.getString("for_examination"));
                b.setCategory(rs.getString("publication_type"));

                b.setCategory_lk_display(rs.getString("publication_type_desc"));
                b.setImportBook(rs.getString("imported_book"));
                b.setSubject01(rs.getString("typical_subject_range_id"));
                b.setSubject02(rs.getString("typical_subject_sub_range_id"));
                b.setSubject03(rs.getString("typical_course_range_id"));
                b.setSpecialty(rs.getString("typical_speciality_range"));
                b.setSpecial_lk_display(rs.getString("typical_speciality_range_id"));
                b.setDownStatus(rs.getString("status"));
                b.setDataClean(rs.getString("clean"));
                b.setIsbn(rs.getString("isbn"));
                b.setResponsibleEditor(rs.getString("chbj"));
                b.setCarrierNum(rs.getString("ztsl"));
                b.setSourceCarrierBookID(rs.getString("kbwxx"));
                b.setReplacedBookID(rs.getString("tdpzsh"));
                b.setCourseType(rs.getString("kclx_id"));

                b.setTempTitle(rs.getString("xtmc"));
                b.setAssessPrice(rs.getString("gjdj"));

                return b;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }


    public void doPost(PortalBook book) throws UnsupportedEncodingException {

        if (StringUtils.isBlank(book.getWbs())) {
            log.error("book wbs not exist" + book.getWbs() + " " + book.getBookID());
        }
        String uriAPI = POSTURL;
        String result = "";
        HttpPost httpRequst = new HttpPost(uriAPI);//创建HttpPost对象

//        List<NameValuePair> params = new ArrayList<NameValuePair>();
//        params.add(new BasicNameValuePair("bookID", "12345-00"));

        MultipartEntity entity = new MultipartEntity();

        if (StringUtils.isNotBlank(book.getZip())) {
            FileBody cover = new FileBody(new File(book.getZip()));

            entity.addPart("UpLoadFile", cover);

            if (StringUtils.isNotBlank(book.getCover())) {
                entity.addPart("coverPath", new StringBody(book.getCover()));
            }
            if (StringUtils.isNotBlank(book.getIntroduce())) {
                entity.addPart("introducePath", new StringBody(book.getIntroduce()));
            }


            if (StringUtils.isNotBlank(book.getToc())) {
                entity.addPart("tocPath", new StringBody(book.getToc()));
            }

            if (StringUtils.isNotBlank(book.getPdf())) {
                entity.addPart("pdfPath", new StringBody(book.getPdf()));
            }
        }


        if (StringUtils.isNotBlank(book.getBookID()))
            entity.addPart("bookID", new StringBody(book.getBookID(), Charset.forName("UTF-8")));
        else
            entity.addPart("bookID", new StringBody("", Charset.forName("UTF-8")));

        if (StringUtils.isNotBlank(book.getSubBookID()))
            entity.addPart("subBookID", new StringBody(book.getSubBookID(), Charset.forName("UTF-8")));

        if (StringUtils.isNotBlank(book.getWbs()))
            entity.addPart("wbs", new StringBody(book.getWbs()));
        if (StringUtils.isNotBlank(book.getAuthor()))
            entity.addPart("author", new StringBody(book.getAuthor(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getTitle()))
            entity.addPart("title", new StringBody(book.getTitle(), Charset.forName("UTF-8")));

        if (StringUtils.isNotBlank(book.getDepartmentID_lk_display()))
            entity.addPart("departmentID_lk_display", new StringBody(book.getDepartmentID_lk_display(), Charset.forName("UTF-8")));

        if (StringUtils.isNotBlank(book.getReader()))
            entity.addPart("reader", new StringBody(book.getReader(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getKeyProject01()))
            entity.addPart("keyProject01", new StringBody(book.getKeyProject01(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getKeyProject02()))
            entity.addPart("keyProject02", new StringBody(book.getKeyProject02(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getKeyProject03()))
            entity.addPart("keyProject03", new StringBody(book.getKeyProject03(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getErpSubject_lk_display()))
            entity.addPart("erpSubject_lk_display", new StringBody(book.getErpSubject_lk_display(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getErpSubject()))
            entity.addPart("erpSubject", new StringBody(book.getErpSubject(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsExam()))
            entity.addPart("isExam", new StringBody(book.getIsExam(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsNormal()))
            entity.addPart("isNormal", new StringBody(book.getIsNormal(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsAdult()))
            entity.addPart("isAdult", new StringBody(book.getIsAdult(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsTrain()))
            entity.addPart("isTrain", new StringBody(book.getIsTrain(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getCategory()))
            entity.addPart("category", new StringBody(book.getCategory(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getCategory_lk_display()))
            entity.addPart("category_lk_display", new StringBody(book.getCategory_lk_display(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getImportBook()))
            entity.addPart("importBook", new StringBody(book.getImportBook(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getDataClean()))
            entity.addPart("dataClean", new StringBody(book.getDataClean(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getPrice()))
            entity.addPart("price", new StringBody(book.getPrice().trim(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getFormat()))
            entity.addPart("format", new StringBody(book.getFormat(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getPageNum()))
            entity.addPart("pageNum", new StringBody(book.getPageNum().trim(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsbn()))
            entity.addPart("isbn", new StringBody(book.getIsbn(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSubject01()))
            entity.addPart("subject01", new StringBody(book.getSubject01(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSubject02()))
            entity.addPart("subject02", new StringBody(book.getSubject02(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSubject03()))
            entity.addPart("subject03", new StringBody(book.getSubject03(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSpecialty()))
            entity.addPart("specialty", new StringBody(book.getSpecialty(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSpecial_lk_display()))
            entity.addPart("special_lk_display", new StringBody(book.getSpecial_lk_display(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getPublishTime()))
            entity.addPart("publishTime", new StringBody(book.getPublishTime(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getPlanPublishTime()))
            entity.addPart("planPublishTime", new StringBody(book.getPlanPublishTime(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getCarrierType()))
            entity.addPart("carrierType", new StringBody(book.getCarrierType(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getDepartmentID()))
            entity.addPart("departmentID", new StringBody(book.getDepartmentID().trim(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getReplacedBookID()))
            entity.addPart("replacedBookID", new StringBody(book.getReplacedBookID(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getResponsibleEditor()))
            entity.addPart("responsibleEditor", new StringBody(book.getResponsibleEditor(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getCarrierNum()))
            entity.addPart("carrierNum", new StringBody(book.getCarrierNum().trim(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getWords()))
            entity.addPart("words", new StringBody(book.getWords().trim(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getPrintColor()))
            entity.addPart("printColor", new StringBody(book.getPrintColor(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getBinding()))
            entity.addPart("binding", new StringBody(book.getBinding(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getReferenceEditor()))
            entity.addPart("referenceEditor", new StringBody(book.getReferenceEditor(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getResourceType()))
            entity.addPart("resourceType", new StringBody(book.getResourceType(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getCopyright()))
            entity.addPart("copyright", new StringBody(book.getCopyright(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getVersion()))
            entity.addPart("version", new StringBody(book.getVersion(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getVolumns()))
            entity.addPart("volumns", new StringBody(book.getVolumns(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getLanguage()))
            entity.addPart("language", new StringBody(book.getLanguage(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSeries()))
            entity.addPart("series", new StringBody(book.getSeries(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSeriesName()))
            entity.addPart("seriesName", new StringBody(book.getSeriesName(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getPublishStatus()))
            entity.addPart("publishStatus", new StringBody(book.getPublishStatus(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getEdition()))
            entity.addPart("edition", new StringBody(book.getEdition().trim(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getCourseType()))
            entity.addPart("courseType", new StringBody(book.getCourseType(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getCourseType_lk_display()))
            entity.addPart("courseType_lk_display", new StringBody(book.getCourseType_lk_display(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getStudyURL()))
            entity.addPart("studyURL", new StringBody(book.getStudyURL(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getTempTitle()))
            entity.addPart("tempTitle", new StringBody(book.getTempTitle(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getImpression()))
            entity.addPart("impression", new StringBody(book.getImpression(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getPrintDate()))
            entity.addPart("printDate", new StringBody(book.getPrintDate(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getDownStatus()))
            entity.addPart("downStatus", new StringBody(book.getDownStatus(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSourceCarrierBookID()))
            entity.addPart("sourceCarrierBookID", new StringBody(book.getSourceCarrierBookID(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getErpBrief()))
            entity.addPart("erpBrief", new StringBody(book.getErpBrief(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getAssessPrice()))
            entity.addPart("assessPrice", new StringBody(book.getAssessPrice(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getCmsId()))
            entity.addPart("cmsId", new StringBody(book.getCmsId(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsAbook()))
            entity.addPart("isAbook", new StringBody(book.getIsAbook(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsEbook()))
            entity.addPart("isEbook", new StringBody(book.getIsEbook(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsAbookCourse()))
            entity.addPart("isAbookCourse", new StringBody(book.getIsAbookCourse(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getAbookURL()))
            entity.addPart("abookURL", new StringBody(book.getAbookURL(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getTerm()))
            entity.addPart("term", new StringBody(book.getTerm(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSubTitle()))
            entity.addPart("subTitle", new StringBody(book.getSubTitle(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getAbookBrief()))
            entity.addPart("abookBrief", new StringBody(book.getAbookBrief(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getAbookCoverPath()))
            entity.addPart("abookCoverPath", new StringBody(book.getAbookCoverPath(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getAuthorBrief()))
            entity.addPart("authorBrief", new StringBody(book.getAuthorBrief(), Charset.forName("UTF-8")));

        try {
            httpRequst.setEntity(entity);
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequst);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                HttpEntity httpEntity = httpResponse.getEntity();
                result = EntityUtils.toString(httpEntity);//取出应答字符串
                System.out.println(result);
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result = e.getMessage().toString();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result = e.getMessage().toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result = e.getMessage().toString();
        }
//        return result;
    }

    public boolean prepareCover1(PortalBook book) throws Exception {

        String cover = CMSUtil.getCoverPath(book.getCmsId());

        if (StringUtils.isNotBlank(cover)&&(cover.contains(".jpg")||cover.contains(".JPG")||cover.contains(".png")||cover.contains(".PNG"))) {

            //wget
            File dst = new File(baseFile + "/" + book.getCmsId()+ "/cover/" + book.getCmsId() + "_front.jpg");

            try {
                FileUtils.copyURLToFile(new URL(cover), dst);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

//            图片压缩
            try {
                ScaleImage si=new ScaleImage();
                si.saveImageAsJpg(dst.getPath(), dst.getPath(), 180, 240);
            } catch (Exception e) {
                e.printStackTrace();
//                return false;
            }

            //打包

//            String zip = baseFile + "/" + book.getBookID() + "/" + book.getBookID() + ".zip";
//            ZipUtil.zip(zip, dst.getPath().replace("\\","/"));

            book.setCover(book.getCmsId() + "/cover/" + book.getCmsId() + "_front.jpg");
            return true;
        }
        return false;

    }

    public boolean prepareIntroduce1(PortalBook book) throws Exception {

        String intro = CMSUtil.getBrief(book.getCmsId());

        if (StringUtils.isNotBlank(intro)) {

            File tocFile = new File(baseFile + "/" + book.getCmsId() + "/introduce/index.html");
            FileUtils.writeStringToFile(tocFile, intro.replaceAll("\\\\\"","\"").replaceAll("\\\\r","\r").replaceAll("\\\\n","\n"), "UTF-8", true);

            book.setIntroduce(book.getCmsId() + "/introduce/index.html");

            return true;

        }
        return false;

    }


    public boolean prepareToc1(PortalBook book) throws Exception {

        String toc = CMSUtil.getToc(book.getCmsId());

        if (StringUtils.isNotBlank(toc)) {

            File tocFile = new File(baseFile + "/" + book.getCmsId() + "/toc/index.html");
            FileUtils.writeStringToFile(tocFile, toc, "UTF-8", true);

            book.setToc(book.getCmsId() + "/toc/index.html");

            return true;

        }
        return false;

    }

    public boolean preparePdf1(PortalBook book) throws Exception {

        List<HashMap> pdfList = CMSUtil.getPdf(book.getCmsId());
        String pdfPath="";

        if (pdfList!=null&&pdfList.size()!=0) {
            for (HashMap<String,String> pdfMap : pdfList) {
                String name=pdfMap.get("name");
                String url=pdfMap.get("url");
                File dst = new File(baseFile + "/" + book.getCmsId()+ "/pdf/" + name);

                try {
                    FileUtils.copyURLToFile(new URL(url), dst);
                    if(!"".equals(pdfPath))
                        pdfPath=pdfPath+",";
                    pdfPath=pdfPath+book.getCmsId() + "/pdf/" + name;

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

            }

            book.setPdf(pdfPath);
            return true;

        }
        return false;

    }

    public void doPost1(PortalBook book) throws UnsupportedEncodingException {

        if (StringUtils.isBlank(book.getCmsId())) {
            log.error("book cmsId not exist" + book.getCmsId());
        }
        String uriAPI = POSTURL1;
        String result = "";
        HttpPost httpRequst = new HttpPost(uriAPI);//创建HttpPost对象

//        List<NameValuePair> params = new ArrayList<NameValuePair>();
//        params.add(new BasicNameValuePair("bookID", "12345-00"));

        MultipartEntity entity = new MultipartEntity();

        if (StringUtils.isNotBlank(book.getZip())) {
            FileBody cover = new FileBody(new File(book.getZip()));

            entity.addPart("UpLoadFile", cover);

            if (StringUtils.isNotBlank(book.getCover())) {
                entity.addPart("coverPath", new StringBody(book.getCover()));
            }
            if (StringUtils.isNotBlank(book.getIntroduce())) {
                entity.addPart("introducePath", new StringBody(book.getIntroduce()));
            }


            if (StringUtils.isNotBlank(book.getToc())) {
                entity.addPart("tocPath", new StringBody(book.getToc()));
            }

            if (StringUtils.isNotBlank(book.getPdf())) {
                entity.addPart("pdfPath", new StringBody(book.getPdf()));
            }
        }

        if (StringUtils.isNotBlank(book.getWbs()))
            entity.addPart("wbs", new StringBody(book.getWbs()));
        if (StringUtils.isNotBlank(book.getBookID()))
            entity.addPart("bookID", new StringBody(book.getBookID()));
        if (StringUtils.isNotBlank(book.getErpBrief()))
            entity.addPart("erpBrief", new StringBody(book.getErpBrief(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getCmsId()))
            entity.addPart("cmsId", new StringBody(book.getCmsId(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsAbook()))
            entity.addPart("isAbook", new StringBody(book.getIsAbook(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsEbook()))
            entity.addPart("isEbook", new StringBody(book.getIsEbook(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getIsAbookCourse()))
            entity.addPart("isAbookCourse", new StringBody(book.getIsAbookCourse(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getAbookURL()))
            entity.addPart("abookURL", new StringBody(book.getAbookURL(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getTerm()))
            entity.addPart("term", new StringBody(book.getTerm(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSeriesName()))
            entity.addPart("seriesName", new StringBody(book.getSeriesName(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getSubTitle()))
            entity.addPart("subTitle", new StringBody(book.getSubTitle(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getAbookBrief()))
            entity.addPart("abookBrief", new StringBody(book.getAbookBrief(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getAbookCoverPath()))
            entity.addPart("abookCoverPath", new StringBody(book.getAbookCoverPath(), Charset.forName("UTF-8")));
        if (StringUtils.isNotBlank(book.getAuthorBrief()))
            entity.addPart("authorBrief", new StringBody(book.getAuthorBrief(), Charset.forName("UTF-8")));

        try {
            httpRequst.setEntity(entity);
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequst);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                HttpEntity httpEntity = httpResponse.getEntity();
                result = EntityUtils.toString(httpEntity);//取出应答字符串
                System.out.println(result);
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result = e.getMessage().toString();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result = e.getMessage().toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result = e.getMessage().toString();
        }
//        return result;
    }


    public Connection getHepmdConn() {
        if (hepmdConn == null) {
//            System.out.println(111);
            DbUtil cmsDb = new DbUtil();
//            cmsDb.setDburl("jdbc:oracle:thin:@10.3.10.14:1521:WIND");
            cmsDb.setDburl("jdbc:oracle:thin:@192.168.75.186:1521:WIND");
            cmsDb.setSDBDriver("oracle.jdbc.driver.OracleDriver");
            cmsDb.setUser("hepmd");
            cmsDb.setPasswd("hepmd");
            hepmdConn = cmsDb.getConn();
        }

        return hepmdConn;
    }

    public Connection getHepoutConn() {
        if (hepoutConn == null) {
            DbUtil cmsDb = new DbUtil();
            cmsDb.setDburl("jdbc:oracle:thin:@10.3.10.14:1521:WIND");
            cmsDb.setSDBDriver("oracle.jdbc.driver.OracleDriver");
            cmsDb.setUser("hepout");
            cmsDb.setPasswd("hepout");
            hepoutConn = cmsDb.getConn();
        }

        return hepoutConn;
    }

    public String getWbsByCmsId(String cmsId) throws Exception {

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(new URL("http://hep66.hepportal/xpe/cmcore/simpleSearch?select=wbs&contentType=hep/book&cmsId="+cmsId));
            Element root = doc.getRootElement();
            List resources = root.getChildren();
            for (int i = 0; i < resources.size(); i++) {
                Element resource = (Element) resources.get(i);
                List metadatas = resource.getChildren();
                for (int j = 0; j < metadatas.size(); j++) {
                    Element metadata = (Element) metadatas.get(j);
                    String value = metadata.getText();
                    return value;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "";

    }

    public static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    @Test
    public void syncPdf() throws Exception {


        List<String> resIdList = CMSUtil.getCMSIdsByUpdated("20140201021839");
        List<String> wbss = new ArrayList<String>();
        System.out.println(resIdList.size());

        List pdfList=new ArrayList<HashMap>();

        int i=0;
        for (String cmsId : resIdList) {
            System.out.println(i++);
            try {

                String cover = CMSUtil.getToc(cmsId);

                if (StringUtils.isNotBlank(cover))
                log.info(cmsId);

            } catch (Exception e) {
                continue;
            }

        }
    }

}
