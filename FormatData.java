package com.hep;

import com.softtouchit.xpe.util.filter.AbstractUtilFilter;
import com.util.JsoupWrapper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FormatData extends AbstractUtilFilter {

    private static final String NS = "http://www.ulearn.com.cn/xpe/util";
    private static final String prefix = "uxf:";
    private static final String STORE_PATH = "/data/hepcluster/hep/book/unzip/";
//    private static final String STORE_PATH = "/home/xpe/xpe2.0/temp/httpcache/sync/";

    public void startElement(String uri, String localName, String qName, Attributes atts)
            throws SAXException {
        if (uri.equals(NS) && "formatData".equals(localName)) {
            try {
                String data=atts.getValue("data");
                String[] dataArray=data.split("&!&");
                String[] dataName=dataArray[0].split("&;&");
                String[] dataNameValue=dataArray[1].split("&;&");
                 Map<String, String> dataMap=new HashMap<String, String>();
                for(int i=0;i<dataName.length;i++){
                    if("null".equals(dataNameValue[i])){
                        dataMap.put(dataName[i], "");
                    }else{
                        dataMap.put(dataName[i], dataNameValue[i]);
                    }
                }

                String bookID = dataMap.get("bookID");
                String publishTime = dataMap.get("publishTime");
                String filePath=atts.getValue("filePath");
                if(!"".equals(filePath)){
//                    unZip1(filePath.substring(5),STORE_PATH);
//                    unZip(filePath.substring(5),STORE_PATH+bookID+"/");
                    unZip(filePath.substring(5),STORE_PATH);
//                    unZip2("/home/xpe/xpe2.0/temp/httpcache/14683cb60a37634/111.zip",STORE_PATH);
                }
 
                String isbn = dataMap.get("isbn");
                String sourceCarrierBookID = dataMap.get("sourceCarrierBookID");
                Pattern pattern = Pattern.compile("[0-9]*");


                if (!"".equals(bookID) && bookID.split("-").length == 2 && bookID.substring(bookID.indexOf("-") + 1).length() == 3 && pattern.matcher(bookID.substring(bookID.indexOf("-") + 1)).matches()) {
                    return;
                }
                
                String departmentID = dataMap.get("departmentID");

                if(departmentID.startsWith("1010")||departmentID.startsWith("001010")){
                        dataMap.put("contentType", "hep/product");
                        dataMap.put("DataSource", "import");
                        dataMap.put("DataSource_lk_display", "数据同步");
//                        if("".equals(isbn)){
//                            dataMap.put("isPublished", "no");
//                        }else if("".equals(sourceCarrierBookID)){
//                            dataMap.put("isPublished", "yes");
//                        }else{
//                            return;
//                        }
                    }else{
                        dataMap.put("contentType", "hep/book");
                        if(bookID==null||publishTime==null||"".equals(bookID)||"".equals(publishTime)){
                            dataMap.put("isPublished", "no");
                        }else{
                            dataMap.put("isPublished", "yes");
                        }
                    }
                    if(departmentID.startsWith("00")){
                        dataMap.put("departmentID", departmentID.substring(2));
                    }

                AttributesImpl _count = new AttributesImpl();
                _count.addAttribute("", "count", "count", "CDATA", "1");
                super.startElement("", "result", "result", _count);
                bulidResult(dataMap);
                super.endElement("", "result", "result");
            } catch (Exception e) {
                super.startElement(uri, localName, qName, atts);
                handleException(e);
            }

        } else {
            super.startElement(uri, localName, qName, atts);
        }
    }

    public void endElement(String uri, String localName, String qName)
			throws SAXException {

        if (uri.equals(NS) && "formatData".equals(localName)) {
            return;
        }else{
            super.endElement(uri, localName, qName);
        }

    }

    public void bulidResult(Map<String, String> dataMap) throws SAXException, Exception{

        String[] contentName1 = {"title" ,"bookID", "subBookID", "wbs", "isbn", "departmentID", "departmentID_lk_display", "author",
                "subject01","subject02","subject03","price", "format", "edition", "words", "pageNum", "publishTime","planPublishTime",
                "printColor", "binding", "category","category_lk_display", "erpSubject","erpSubject_lk_display", "isNormal","isAdult",
                "isTrain","isExam", "keyProject01", "keyProject02", "keyProject03","specialty","specialty_lk_display",
                "responsibleEditor", "referenceEditor", "resourceType", "copyright", "series","erpBrief","assessPrice",
                "version", "volumns", "language", "importBook", "carrierType", "carrierNum", "reader","downStatus",
                "dataClean", "publishStatus", "replacedBookID", "courseType", "courseType_lk_display","studyURL","term","subTitle",
                "tempTitle","impression","printDate","baseUpdateTime","contentType","sourceCarrierBookID","isPublished",
                "DataSource","DataSource_lk_display","coverPath", "introducePath","tocPath","pdfPath","cmsId","isAbook","isEbook",
                "isAbookCourse","abookURL","seriesName","abookBrief","abookCoverPath","authorBrief"};

//        String[] contentName1 = {"wbs","downStatus","cmsId","contentType"};

        AttributesImpl _name = null;

        _name = new AttributesImpl();
        _name.addAttribute("", "seq", "seq", "CDATA", "");
        super.startElement("", "resource", "resource", _name);

        if (dataMap != null) {
            if("0.00 ".equals(dataMap.get("assessPrice"))||"0.00".equals(dataMap.get("assessPrice")))
                dataMap.put("assessPrice",dataMap.get("price"));
            for (String content : contentName1) {
                if (dataMap.get(content) != null) {
                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", content);
                    super.startElement("", "metadata", "metadata", _name);
                    String value = dataMap.get(content);
                    super.characters(value.toCharArray(), 0, value.toCharArray().length);
                    super.endElement("", "metadata", "metadata");
                }
            }
            if (dataMap.get("isbn") != null) {
                String isbn = dataMap.get("isbn").replaceAll("-", "");
                String value = "";

                value = sysDangdangUrl(isbn);
                if (!"".equals(value)) {
                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", "isDang");
                    super.startElement("", "metadata", "metadata", _name);
                    super.characters("yes".toCharArray(), 0, "yes".toCharArray().length);
                    super.endElement("", "metadata", "metadata");

                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", "dangURL");
                    super.startElement("", "metadata", "metadata", _name);
                    super.characters(value.toCharArray(), 0, value.toCharArray().length);
                    super.endElement("", "metadata", "metadata");
                }else{
                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", "isDang");
                    super.startElement("", "metadata", "metadata", _name);
                    super.characters("no".toCharArray(), 0, "no".toCharArray().length);
                    super.endElement("", "metadata", "metadata");
                }

                value = sysAmazonurl(isbn);
                if (!"".equals(value)) {
                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", "isAmazon");
                    super.startElement("", "metadata", "metadata", _name);
                    super.characters("yes".toCharArray(), 0, "yes".toCharArray().length);
                    super.endElement("", "metadata", "metadata");

                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", "amazonURL");
                    super.startElement("", "metadata", "metadata", _name);
                    super.characters(value.toCharArray(), 0, value.toCharArray().length);
                    super.endElement("", "metadata", "metadata");
                }else{
                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", "isAmazon");
                    super.startElement("", "metadata", "metadata", _name);
                    super.characters("no".toCharArray(), 0, "no".toCharArray().length);
                    super.endElement("", "metadata", "metadata");
                }

                value = sysjingdongUrl(isbn);
                if (!"".equals(value)) {
                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", "isJd");
                    super.startElement("", "metadata", "metadata", _name);
                    super.characters("yes".toCharArray(), 0, "yes".toCharArray().length);
                    super.endElement("", "metadata", "metadata");

                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", "jdURL");
                    super.startElement("", "metadata", "metadata", _name);
                    super.characters(value.toCharArray(), 0, value.toCharArray().length);
                    super.endElement("", "metadata", "metadata");
                }else{
                    _name = new AttributesImpl();
                    _name.addAttribute("", "name", "name", "CDATA", "isJd");
                    super.startElement("", "metadata", "metadata", _name);
                    super.characters("no".toCharArray(), 0, "no".toCharArray().length);
                    super.endElement("", "metadata", "metadata");
                }
            }
        }




        super.endElement("", "resource", "resource");

    }

    public static void unZip1(String path,String savepath) {
        int count = -1;
        int index = -1;

//        String savepath = path.substring(0, path.lastIndexOf("."));
//        String savepath = STORE_PATH;

        try {
            BufferedOutputStream bos = null;
            ZipEntry entry = null;
            FileInputStream fis = new FileInputStream(path);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));

            while ((entry = zis.getNextEntry()) != null) {

                if (entry.isDirectory()) {
                    new File(savepath + "/" + entry.getName()).mkdirs();
                    continue;
                }

                byte data[] = new byte[2048];

                File f = new File(savepath + "/" + entry.getName());
                f.createNewFile();

                FileOutputStream fos = new FileOutputStream(f);
                bos = new BufferedOutputStream(fos, 2048);

                while ((count = zis.read(data, 0, 2048)) != -1) {
                    bos.write(data, 0, count);
                }

                bos.flush();
                bos.close();
            }

            zis.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unZip(String fileName,String desDir){
    	try{
            ZipFile zipFile = new ZipFile(fileName);
            Enumeration enu = zipFile.entries();
//            String desDir=fileName.substring(0,fileName.lastIndexOf("."));
            new File(desDir).mkdirs();
            int i = 0;

            while(enu.hasMoreElements()){
                ZipEntry zipEntry = (ZipEntry)enu.nextElement();
                if(zipEntry.isDirectory()){
                	new File(desDir+"/"+zipEntry.getName()).mkdirs();
                    continue;
                }
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                File file = new File(desDir+"/"+zipEntry.getName());
                File parent = file.getParentFile();
                if(parent != null && !parent.exists()){
                    parent.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos,2048);

                int count;
                byte[] array = new byte[2048];
                while((count = bis.read(array, 0, 2048))!=-1){
                    bos.write(array, 0, count);
                }

                bos.flush();
                bos.close();
                bis.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void unZip2(String fileName,String desDir){
    	try{
            ZipFile zipFile = new ZipFile(fileName);
            Enumeration enu = zipFile.entries();
//            String desDir=fileName.substring(0,fileName.lastIndexOf("."));
            new File(desDir).mkdirs();
            int i = 0;

            while(enu.hasMoreElements()){
                ZipEntry zipEntry = (ZipEntry)enu.nextElement();
                if(zipEntry.isDirectory()){
                	new File(desDir+"/"+zipEntry.getName()).mkdirs();
                    continue;
                }
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                File file = new File(desDir+"/"+zipEntry.getName());
                File parent = file.getParentFile();
                if(parent != null && !parent.exists()){
                    parent.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos,2048);

                int count;
                byte[] array = new byte[2048];
                while((count = bis.read(array, 0, 2048))!=-1){
                    bos.write(array, 0, 2048);
                }

                bos.flush();
                bos.close();
                bis.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public String sysAmazonurl(String isbn) throws Exception {

            Thread.currentThread().sleep(10);

            try {
               if (!(isbn == null || isbn.length() == 0 || isbn.equals("null"))) {
//                    String dangdang = "http://www.amazon.cn/gp/search/ref=sr_adv_b/?search-alias=stripbooks&field-isbn=" + isbn;
                    String dangdang = "https://www.amazon.cn/s?i=stripbooks&__mk_zh_CN=%E4%BA%9A%E9%A9%AC%E9%80%8A%E7%BD%91%E7%AB%99&ref=nb_sb_noss&k=" + isbn;
                    org.jsoup.nodes.Document doc = JsoupWrapper.INSTANCE.get(dangdang);
                    org.jsoup.nodes.Element nono = doc.getElementById("noResultsTitle");
                    if (nono != null && nono.text().contains("没有找到")) {
                        return "";
                    }
                    org.jsoup.nodes.Element tmp = doc.select("[class=a-color-base s-line-clamp-2]").first();

                    if (tmp != null) {
                        return "https://www.amazon.cn"+tmp.getElementsByTag("a").first().attr("href");

                    }

                }
            }catch (Exception e) {
                e.printStackTrace();
                return "";
            }

        return "";

    }

    public String sysDangdangUrl(String isbn) throws Exception {


            Thread.currentThread().sleep(100);

            try {
            if (!(isbn == null || isbn.length() == 0 || isbn.equals("null"))) {
                String dangdang = "http://search.dangdang.com/?key=" + isbn;


                    org.jsoup.nodes.Document doc = JsoupWrapper.INSTANCE.get(dangdang);
                    org.jsoup.nodes.Element nono = doc.select("[class=search_null]").first();


                    if (nono != null && nono.getElementsByTag("h1").text().contains("没有找到")) {
                        return "";
                    }
                    org.jsoup.nodes.Element tmp = doc.select("[class=line1]").first();

                    if (tmp != null) {
                        return tmp.getElementsByTag("a").first().attr("href");
                    }

            }
            }catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        return "";

    }

    public String sysjingdongUrl(String isbn) throws Exception {

            Thread.currentThread().sleep(10);

            try {
            if (!(isbn == null || isbn.length() == 0 || isbn.equals("null"))) {
                String dangdang = "http://search.jd.com/search?enc=utf-8&qr=&qrst=UNEXPAND&rt=1&book=y&wtype=1&keyword=" + isbn+"#filter";


                org.jsoup.nodes.Document doc = JsoupWrapper.INSTANCE.get(dangdang);
                org.jsoup.nodes.Element nono = doc.select("[class=nf-content]").first();


                if (nono != null && nono.getElementsByTag("span").text().contains("没有找到")) {
                    return "";
                }
                org.jsoup.nodes.Element tmp = doc.select("[class=p-img]").first();
                org.jsoup.nodes.Element source = doc.select("[class=p-shopnum]").first();

                if (tmp != null && source.getElementsByTag("a").last().text().contains("高等教育出版社")) {
                    return tmp.getElementsByTag("a").first().attr("href");
                }


            }
            }catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        return "";

    }


}

