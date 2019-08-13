<%@ page import="org.apache.commons.io.FileUtils" %>
<%@ page import="java.io.File" %>
<%@ page import="com.alibaba.fastjson.JSONArray" %>
<%@ page import="com.alibaba.fastjson.JSON" %>
<%@ page import="com.alibaba.fastjson.JSONObject" %>
<%--
  Created by IntelliJ IDEA.
  User: david
  Date: 12-12-25
  Time: 下午3:16
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<title>成品书编校质量抽检系统</title>

<script type="text/javascript" src="js/jquery-latest.js"></script>
<script type="text/javascript" src="js/taffy.js"></script>
<link rel='stylesheet' href='images/button/gh-buttons.css' type='text/css' media='all'/>

<style type="text/css">
    body {
        background-color: #1a1a1a;
    }

    .nu01 {
        font-family: "Times New Roman", Times, serif;
        font-size: 140px;
        font-weight: bold;
        color: #FFF;
        height: 150px;
        width: 1100px;
        padding-top: 10px;
        padding-right: 10px;
        padding-bottom: 10px;
        padding-left: 10px;
        margin: 10px;
        letter-spacing: 60px;
        text-align: left;
    }

    .frame {
        height: 200px;
        width: 990px;
        margin-bottom: 6px;
        margin-left: 40px;
        background-image: url(images/fbtn.jpg);
        background-repeat: no-repeat;
    }

    .title {
        font-family: "黑体";
        font-size: 40px;
        color: #FC0;
        font-weight: bold;
        padding: 20px;
    }

    .listCopy {
        height: 30px;
        width: 990px;
        margin-bottom: 10px;
        font-family: Verdana, Geneva, sans-serif;
        font-size: 24px;
        color: #FFF;
        overflow: hidden;
    }

    .result {
        border: medium double #FC0;
        padding: 10px;
        width: 1200px;
    }

    .tablesorter {
        width: 98%;
    }

    .tablesorter td {
        color: white;
        font-size: 150%;
        border: 1px solid gray;
    }

    .tablesorter th {
        color: white;
        font-size: 150%;
        font-weight: bolder;
        border: 1px solid gray;
    }


</style>


<!--
首先把理工的物料号取出来吧
-->

<%
    String bm = request.getParameter("bm");
    bm = new String(bm.getBytes("ISO8859-1"), "UTF-8");
    String path = request.getSession().getServletContext().getRealPath("/");
    String total = FileUtils.readFileToString(new File(path + "/result.json"), "UTF-8");
    JSONArray jsonArray = JSON.parseArray(total);


    JSONArray result = new JSONArray();

    for (int i = 0; i < jsonArray.size(); i++) {

        JSONObject jsonObject = JSON.parseObject(jsonArray.get(i).toString());
        if (jsonObject.get("syb").equals(bm)) {
            result.add(jsonObject);


        }

    }


    //下面的抽奖 就是根据这个result进行处理

%>

<script type="text/javascript">

    //Array Remove - By John Resig (MIT Licensed)
    Array.prototype.remove = function (from, to) {
        var rest = this.slice((to || from) + 1 || this.length);
        this.length = from < 0 ? this.length + from : from;
        return this.push.apply(this, rest);
    };
    String.prototype.trim = function () {
        return this.replace(/(^\s*)(\s*$)/g, "");
    }

    var timetodo;
    var sourceList = <%=result%>;
    var resultList;

    var lucknum;

    var shuffleok = "";


    $(document).ready(function () {
        //更新概率
        updateSubtitle();

    });

    function updateSubtitle() {
        $.get("guess.jsp", {old: shuffleok, total: '<%=bm%>'}, function (result) {
            $("#subtitle").html(result);
        });
    }

    function updateNowWuliao() {
        shuffleok = "";
        $("[class='sswuliao']").each(function () {
            shuffleok += $(this).html() + ",";
        });

    }

    function exp() {
        updateNowWuliao();


        var myInput = document.getElementById("swuliao");
        myInput.setAttribute("value", shuffleok);
        myForm.submit();
    }


    function st() {
        $("#bt").children(0).attr("src", "images/start01.jpg");
        $("#bt").attr("href", "javascript:go();");
        clearTimeout(timetodo);
        $.get("do.jsp", {num: $("#onenum").attr("value"), old: shuffleok, total: '<%=bm%>'}, function (result) {

            resultList = eval(result);
            refreshlist();
            updateNowWuliao();
//            $("#count").html("<font color=red>" + (sourceList.length - resultList.length) + "</font>");
            updateSubtitle();
        });
    }


    function changelist(row) {
        $("#luck" + row + "").empty();
        var luck;
        try {
            luck = eval("(" + resultList[row] + ")");
        } catch (e) {
            luck = resultList[row];
        }
        var a = row;
        a++;
        $("#luck" + row + "").append("<td>" + (a) + "</td><td class='sswuliao'>" + luck.wuliao + "</td><td>" + luck.zrbj + "</td><td>" + luck.bhzdsm + "</td><td><a href=\"javascript:changethisrow('" + row + "')\";  class='button'>替换</a></td>");

    }


    function refreshlist() {
        $("#goodluckbody").empty();
        for (i = 0; i < resultList.length; i++) {
            var luck
            try {
                luck = eval("(" + resultList[i] + ")");
            } catch (e) {
                luck = resultList[i];
            }

            var rowstr = "<tr id='luck" + i + "' ";
            if (luck.bb == "QB") {
                rowstr += " style='color:red;background:darkred' ";
            }

//            rowstr += "><td>" + (i + 1) + "</td><td class='sswuliao'>" + luck.wuliao + "</td><td>" + luck.zrbj + "</td><td>" + luck.bhzdsm + "</td><td><a href=\"javascript:changethisrow('" + i + "')\";  class='button'>替换</a> <a href=\"javascript:blackthisrow('" + i + "')\";  class='button'>黑名单</a></td></tr>";
//            rowstr += "><td>" + (i + 1) + "</td><td class='sswuliao'>" + luck.wuliao + "</td><td>" + luck.zrbj + "</td><td>" + luck.bhzdsm + "</td><td><a href=\"javascript:blackthisrow('" + i + "')\";  class='button'>黑名单</a></td></tr>";

            $("#goodluckbody").append(rowstr);
            $("#imgshuffle").html(luck.wuliao);
        }
    }
    function go() {
        $("#bt").children(0).attr("src", "images/stop01.jpg");
        $("#bt").attr("href", "javascript:st();");
        timetodo = setTimeout("show()", 50);
        console.log('aaa');
    }


    function changethisrow(row) {
        var luck = eval("(" + resultList[row] + ")");
        updateNowWuliao();
        $.get("change.jsp", {old: shuffleok, total: '<%=bm%>', change: luck.wuliao, black: $("#black").html()}, function (result) {
            resultList = eval(result);
            refreshlist();
            updateNowWuliao();
            updateSubtitle();
        });
    }
    function blackthisrow(row) {

        var luck = eval("(" + resultList[row] + ")");


        if ($("#black").html().indexOf(luck.wuliao) == -1) {
            $("#bb").show();
            $("#black").append(luck.wuliao + ";");
        }

        updateNowWuliao();
        $.get("change.jsp", {old: shuffleok, total: '<%=bm%>', change: luck.wuliao, black: $("#black").html()}, function (result) {
            resultList = eval(result);
            refreshlist();
            updateNowWuliao();
            updateSubtitle();
        });
    }


    function show() {
        try {
            lucknum = Math.round(Math.random() * sourceList.length);
            console.log(lucknum+' vs' +sourceList.length);
            if (sourceList[lucknum] == undefined) {
                timetodo = setTimeout("show()", 50)
            }
            $("#imgshuffle").html(sourceList[lucknum].wuliao);
            timetodo = setTimeout("show()", 50)
        } catch (err) {

        }
    }


</script>

<script type="text/javascript">
    function MM_swapImgRestore() { //v3.0
        var i, x, a = document.MM_sr;
        for (i = 0; a && i < a.length && (x = a[i]) && x.oSrc; i++) x.src = x.oSrc;
    }
    function MM_preloadImages() { //v3.0
        var d = document;
        if (d.images) {
            if (!d.MM_p) d.MM_p = new Array();
            var i, j = d.MM_p.length, a = MM_preloadImages.arguments;
            for (i = 0; i < a.length; i++)
                if (a[i].indexOf("#") != 0) {
                    d.MM_p[j] = new Image;
                    d.MM_p[j++].src = a[i];
                }
        }

//        $("#count").html("<font color=red>" + sourceList.length + "</font>");

    }

    function MM_findObj(n, d) { //v4.01
        var p, i, x;
        if (!d) d = document;
        if ((p = n.indexOf("?")) > 0 && parent.frames.length) {
            d = parent.frames[n.substring(p + 1)].document;
            n = n.substring(0, p);
        }
        if (!(x = d[n]) && d.all) x = d.all[n];
        for (i = 0; !x && i < d.forms.length; i++) x = d.forms[i][n];
        for (i = 0; !x && d.layers && i < d.layers.length; i++) x = MM_findObj(n, d.layers[i].document);
        if (!x && d.getElementById) x = d.getElementById(n);
        return x;
    }

    function MM_swapImage() { //v3.0
        var i, j = 0, x, a = MM_swapImage.arguments;
        document.MM_sr = new Array;
        for (i = 0; i < (a.length - 2); i += 3)
            if ((x = MM_findObj(a[i])) != null) {
                document.MM_sr[j++] = x;
                if (!x.oSrc) x.oSrc = x.src;
                x.src = a[i + 2];
            }
    }
</script>
</head>

<body onload="MM_preloadImages('images/start02.jpg','images/result02.jpg')">
<%--<div class="title"><%=bm%>目前还有图书<span id="count"></span>本</div>--%>
<div id="subtitle" style="color:white;margin-left: 1em;display:none"></div>

<div class="frame">

    <div class="nu01">
        <div id="imgshuffle">12345-00</div>
    </div>
</div>
<table width="800" border="0" cellpadding="5" cellspacing="5">
    <tr>
        <td>
            <div style="color: white;"><a href="javascript:window.location.reload();" class="button danger">重新抽取</a>
                抽检<input type="text" size="2" id="onenum"
                         name="onenum" style="padding: 5px;font-size:15pt"
                         value="1"/>本
            </div>
        </td>
        <td width="30%"><a href="javascript:go();" id="bt"><img
                src="images/start01.jpg" name="Image1" border="0"/></a></td>
        <td width="30%"><a href="javascript:exp();" onmouseout="MM_swapImgRestore()"
                           onmouseover="MM_swapImage('Image2','','images/result02.jpg',1)"><img
                src="images/result01.jpg" name="Image2" border="0" id="Image2"/></a></td>

    </tr>
</table>
<div style="color:white;display:none" id="bb"> 黑名单 :<span id="black"></span></div>

<table id="goodluck" class="tablesorter" border="0" cellpadding="0" cellspacing="1" style="overflow: hidden">
    <thead>
    <tr>
        <th nowrap="true" width="80">序号</th>
        <th nowrap="true" width="180">物料</th>
        <th nowrap="true" width="150">责任编辑</th>
        <th>书名</th>
        <th nowrap="true" width="160">操作</th>
    </tr>
    </thead>
    <tbody id="goodluckbody">
    </tbody>
</table>


<form action="export.jsp" method="post" accept="" id="myForm">

    <input type="hidden" name="swuliao" id="swuliao"/>
</form>

<div id="status"></div>
</body>
</html>