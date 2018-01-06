<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/global/taglib.jsp" %>

<div class="modal fade" id="showUserInfoModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span>
                </button>
                <h4 class="modal-title" id="myModalLabel">个人信息</h4>
            </div>
            <div class="modal-body">
                <form class="form-horizontal" id="userInfoForm" role="form" action="${ctx}/saveSelfInfo"
                      method="post" enctype="multipart/form-data">
                    <input type="hidden" id="userId" name="id">
                    <div class="form-group">
                        <label for="userBigIcon" class="col-sm-2 control-label">头像</label>
                        <div class="col-sm-3">
                            <img id="userBigIcon" name="icon" style="width: 100px;height: 100px" src=""/>
                        </div>
                        <div class="col-sm-7">
                            <span class="btn btn-success fileinput-button">
                                <span>上传头像</span>
                                <input type="file" id="uploadIcon" name="uploadIcon">
                            </span><br><br>
                            <span class="upload-hint">支持PNG、GIF、JPG格式，小于2MB</span><br>
                            <label id="fileName"></label>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="userTel" class="col-sm-2 control-label">手机号码</label>
                        <div class="col-sm-10">
                            <input readonly="readonly" type="text" class="form-control" id="userTel" name="tel">
                        </div>
                    </div>
                    <div class="form-group ">
                        <label for="userName" class="col-sm-2 control-label">昵称</label>
                        <div class="col-sm-10">
                            <input type="text" class="form-control" id="userName" name="name">
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="userEmail" class="col-sm-2 control-label">电子邮箱</label>
                        <div class="col-sm-10">
                            <input type="email" id="userEmail" name="email" class="form-control">
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="userArea" class="col-sm-2 control-label">地区</label>
                        <div class="col-sm-10">
                            <input type="text" class="form-control" id="userArea" name="area">
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="col-sm-2 control-label">性别</label>
                        <div class="col-sm-10">
                            <label class="checkbox-inline">
                                <input type="radio" name="sex" value="男" checked>男
                            </label>
                            <label class="checkbox-inline">
                                <input type="radio" name="sex" value="女">女
                            </label>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="userSign" class="col-sm-2 control-label">签名</label>
                        <div class="col-sm-10">
                            <textarea class="form-control dis_change_textarea" id="userSign" name="sign"
                                      rows="2"></textarea>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" onclick="checkUserInfo()">保存</button>
                <button type="button" class="btn btn-default" data-dismiss="modal">取消</button>
            </div>
        </div>
    </div>
</div>

<script>
    // 提交表单
    function checkUserInfo() {
        // TODO 头像类型、大小验证
        var status = true;
        var icon = document.getElementById("uploadIcon").value;
        if (icon != null) {
            var point = icon.lastIndexOf(".");
            var type = icon.substr(point);
            if(type != ".jpg" && type != ".gif" && type != ".JPG" && type != ".GIF" && type != ".PNG" && type != ".png"){
                toastr.info("图片格式错误!");
                status = false;
            }
        }
        if (status) {
            $("#userInfoForm").submit();
        }
    }

    // 实时更新选中的文件名
    $("input[type='file']").change(function(){
        var file = this.files[0];
        $("#fileName").html("当前选中："+file.name);
        // alert(file.size);
    });

</script>