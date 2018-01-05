<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/global/taglib.jsp" %>

<div>
    <div id="editor">
        <p>欢迎使用 <b>无道云笔记</b></p>
    </div>
    <button id="getJSON">获取JSON</button>
    <button id="setContent">恢复笔记</button>

    <script type="text/javascript">
        var strRegex = "^((https|http|ftp|rtsp|mms)?://)"
            + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" //ftp的user@
            + "(([0-9]{1,3}\.){3}[0-9]{1,3}" // IP形式的URL- 199.194.52.184
            + "|" // 允许IP和DOMAIN（域名）
            + "([0-9a-z_!~*'()-]+\.)*" // 域名- www.
            + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\." // 二级域名
            + "[a-z]{2,6})" // first level domain- .com or .museum
            + "(:[0-9]{1,4})?" // 端口- :80
            + "((/?)|" // a slash isn't required if there is no file name
            + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";

        var E = window.wangEditor;
        var editor = new E('#editor');
        // 回掉成功上传的网络图片的地址
        editor.customConfig.linkImgCallback = function (url) {
            alert("上传图片地址："+url);
        };
        // 对输入链接的校验
        editor.customConfig.linkCheck = function (text, link) {
            var re=new RegExp(strRegex);
            if (re.test(link))
                return true;
            else
                return "链接不合法";
        };
        // 区域失去焦点
        editor.customConfig.onblur = function (html) {
            saveContent(html);
        };

        // Func2: 使用 base64 保存图片
        editor.customConfig.uploadImgShowBase64 = true;

        // Func1: 开启上传图片功能，参数：服务端接口
        // editor.customConfig.uploadImgServer = '/upload';
        // 配置服务器端上传地址
        // editor.customConfig.uploadImgServer = '';
        // // 将图片大小限制为 3M（默认为5M）
        // editor.customConfig.uploadImgMaxSize = 3 * 1024 * 1024;
        // // 限制一次最多上传 5 张图片，默认为10000
        // editor.customConfig.uploadImgMaxLength = 5;
        // //上传图片时可自定义传递一些参数，例如传递验证的token等。参数会被添加到formdata中。
        // // editor.customConfig.uploadImgParams = {
        // //     token: 'abcdef12345'  // 属性值会自动进行 encode ，此处无需 encode
        // // };
        // // 将参数拼接到url上
        // // editor.customConfig.uploadImgParamsWithUrl = true;
        // // 将 timeout 时间改为 3s，默认为10s
        // editor.customConfig.uploadImgTimeout = 3000;

        editor.create();
        // 初始化全屏插件
        E.fullscreen.init('#editor');

        // 获取内容的json
        document.getElementById('getJSON').addEventListener('click', function () {
            var json = editor.txt.getJSON(); // 获取 JSON 格式的内容
            var jsonStr = JSON.stringify(json);
            alert("json：" + jsonStr);
        }, false);

        // TODO 后期加上笔记id
        // 保存笔记
        function saveContent() {
            var content = editor.txt.html();
            $.ajax({
                url : "${ctx}/user/saveArticle",
                type : "post",
                dataType : "text",
                data : {
                    // "id" : ,
                    "data" : content
                },
                async :true,
                success : function(res) {
                },
                error : function(){
                    alert("发生错误");
                }
            });
        }

        // 恢复笔记
        document.getElementById('setContent').addEventListener('click', function () {
            $.ajax({
                    url : "${ctx}/user/showUserNote",
                    type : "post",
                    dataType : "text",
                    data : {
                         "key" : "value"
                    },
                    async :true,
                    success : function(res) {
                        editor.txt.html(res);
                    },
                    error : function(){
                        alert("出现错误!");
                    }
                });
        }, false);
    </script>
</div>