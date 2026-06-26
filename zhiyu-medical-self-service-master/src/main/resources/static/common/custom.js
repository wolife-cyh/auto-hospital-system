/**
 * 发送验证码
 */
function sendEmailCode() {
    let email = $('#userEmail').val();
    if (!email) {
        layer.msg("邮箱不能为空");
        return;
    }
    if (!emailReg(email)) {
        layer.msg("请输入正确的邮箱地址");
        return;
    }
    $.ajax({
        type: "POST",
        url: "login/sendEmailCode",
        data: {
            email: email,
        },
        dataType: "json",
        success: function (data) {
            if (data['code'] !== 'SUCCESS') {
                layer.msg(data['message'])
            } else {
                // 禁用按钮，60秒倒计时
                time("#email-code", 60);
            }
        }
    });
}

/**
 * 注册
 */
function register() {
    let userAccount = $('#userAccount').val();
    let userName = $('#userName').val();
    let userPwd = $('#userPwd').val();
    let userTel = $('#userTel').val();
    let userAge = $('#userAge').val();
    let userSex = $('#userSex').find("option:selected").val();
    let userEmail = $('#userEmail').val();
    let code = $('#code').val();

    if (!userAccount || !userName || !userPwd || !userTel || !userAge || !userEmail || !code) {
        layer.msg("请完整填写信息");
        return;
    }

    $.ajax({
        type: "POST",
        url: "login/register",
        data: {
            userAccount: userAccount,
            userName: userName,
            userPwd: userPwd,
            userTel: userTel,
            userAge: userAge,
            userSex: userSex,
            userEmail: userEmail,
            code: code,
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                setTimeout('reload()', 2000);
            }
        }
    });

}

/**
 * 登录
 */
function login() {
    let loginAccount = $('#loginAccount').val();
    let loginPassword = $('#loginPassword').val();
    if (!loginAccount || !loginPassword) {
        layer.msg("请完整登录信息");
        return;
    }
    $.ajax({
        type: "POST",
        url: "login/login",
        data: {
            userAccount: loginAccount,
            userPwd: loginPassword
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                setTimeout('reload()', 2000);
            }
        }
    });

}

/**
 * 修改资料
 */
function updateProfile() {
    let id = $('#userId').val();
    let userName = $('#userName').val();
    let userTel = $('#userTel').val();
    let userAge = $('#userAge').val();
    let imgPath = $('#img').val();

    if (!userName || !userTel || !userAge) {
        layer.msg("请完整填写信息");
        return;
    }

    $.ajax({
        type: "POST",
        url: "user/saveProfile",
        data: {
            id: id,
            userName: userName,
            userTel: userTel,
            userAge: userAge,
            imgPath: imgPath,
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                setTimeout('reload()', 2000);
            }
        }
    });
}

/**
 * 上传头像
 */
function uploadPhoto() {
    var formdata = new FormData();
    formdata.append("file", $("#img-file").get(0).files[0]);
    $.ajax({
        async: false,
        type: "POST",
        url: "file/upload",
        dataType: "json",
        data: formdata,
        contentType: false,//ajax上传图片需要添加
        processData: false,//ajax上传图片需要添加
        success: function (data) {
            console.log(data);
            layer.msg(data['message']);
            $("#img-preview").attr('src', data['data']);
            $("#img").attr('value', data['data']);
        }
    });
}

/**
 * 修改资料
 */
function updatePassword() {
    let oldPass = $('#oldPassword').val();
    let password1 = $('#password1').val();
    let password2 = $('#password2').val();

    if (!oldPass || !password1 || !password2) {
        layer.msg("请完整填写信息");
        return;
    }

    if (password1 !== password2) {
        layer.msg("两次密码不一致");
        return;
    }

    $.ajax({
        type: "POST",
        url: "user/savePassword",
        data: {
            oldPass: oldPass,
            newPass: password1,
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                setTimeout('reload()', 1000);
            }
        }
    });
}

/**
 * 60秒倒计时
 * @param o
 */
function time(o, wait) {
    if (wait === 0) {
        $(o).attr("disabled", false);
        $(o).html("获取");
    } else {
        $(o).attr("disabled", true);
        $(o).html(wait + "秒");
        wait--;
        setTimeout(function () {
            time(o, wait);
        }, 1000);
    }
}

/**
 * 刷新页面
 */
function reload() {
    window.location.reload();
}

/**
 * 跳转到指定页面
 * @param url
 */
function reloadTo(url) {
    let appCnName = APPLICATION_EN_NAME();
    let href = window.location.href;
    href = href.split("/" + appCnName)[0] + "/" + appCnName + url;
    window.location.href = href;
}

/**
 * 跳转到指定页面
 * @param url
 */
function reloadToGO(url) {
    window.location.href = url;
}

/**
 * 分享网站
 */
function share() {
    alert("请复制链接后分享：" + window.location.href);
}

/**
 * 提交反馈
 */
function feedback() {
    let name = $('#name').val();
    let email = $('#email').val();
    let title = $('#title').val();
    let content = $('#content').val();

    if (!name || !email || !title || !content) {
        layer.msg("请完整填写信息");
        return;
    }

    $.ajax({
        type: "POST",
        url: "feedback/save",
        data: {
            name: name,
            email: email,
            title: title,
            content: content,
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                setTimeout('layer.msg(\'我们已经收到了你的反馈，感谢您的支持！\');', 2000);
                setTimeout('reload()', 4000);
            }
        }
    });
}

/**
 * 保存疾病
 */
function saveIllness() {
    let id = $('#id').val();
    let illnessName = $('#illnessName').val();
    let includeReason = $('#includeReason').val();
    let illnessSymptom = $('#illnessSymptom').val();
    let specialSymptom = $('#specialSymptom').val();
    let kindId = $('#kindId').find("option:selected").val();
    let imgPath = $('#imgPath').val();

    $.ajax({
        type: "POST",
        url: "illness/save",
        data: {
            id: id,
            illnessName: illnessName,
            includeReason: includeReason,
            illnessSymptom: illnessSymptom,
            specialSymptom: specialSymptom,
            kindId: kindId,
            imgPath: imgPath,
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                setTimeout('reload()', 2000);
            }
        }
    });
}

/**
 * 上传本地图片（疾病图片、药品图片等存本地）
 * 调用 /file/local-upload，保存到 uploads/illness/ 目录
 */
function uploadLocalPhoto() {
    var formdata = new FormData();
    formdata.append("file", $("#img-file").get(0).files[0]);
    formdata.append("type", "illness");
    $.ajax({
        async: false,
        type: "POST",
        url: "file/local-upload",
        dataType: "json",
        data: formdata,
        contentType: false,
        processData: false,
        success: function (data) {
            console.log(data);
            layer.msg(data['message']);
            $("#img-preview").attr('src', data['data']);
            $("#imgPath").attr('value', data['data']);
        }
    });
}

/**
 * 保存药品
 */
function saveMedicine() {
    let id = $('#id').val();
    let imgPath = $('#img').val();
    let medicineName = $('#medicineName').val();
    let keyword = $('#keyword').val();
    let medicineBrand = $('#medicineBrand').val();
    let medicinePrice = $('#medicinePrice').val();
    let medicineEffect = $('#medicineEffect').val();
    let interaction = $('#interaction').val();
    let taboo = $('#taboo').val();
    let usAge = $('#usAge').val();
    let medicineType = $('#medicineType').find("option:selected").val();

    $.ajax({
        type: "POST",
        url: "medicine/save",
        data: {
            id: id,
            imgPath: imgPath,
            medicineName: medicineName,
            keyword: keyword,
            medicineBrand: medicineBrand,
            medicinePrice: medicinePrice,
            medicineEffect: medicineEffect,
            interaction: interaction,
            taboo: taboo,
            usAge: usAge,
            medicineType: medicineType,
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                var savedId = (data['data'] && data['data'].id) ? data['data'].id : id;
                if (pendingAssocIllnessIds && pendingAssocIllnessIds.length > 0 && savedId) {
                    $('#id').val(savedId);
                    batchCreateAssociations(savedId, function () {
                        setTimeout('reload()', 1000);
                    });
                } else {
                    setTimeout('reload()', 2000);
                }
            }
        }
    });
}

/**
 * 删除疾病
 * @param id
 */
function deleteIllness(id) {
    $.ajax({
        type: "POST",
        url: "illness/delete",
        data: {
            id: id,
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                setTimeout('reload()', 2000);
            }
        }
    });
}

/**
 * 删除药品
 * @param id
 */
function deleteMedicine(id) {
    $.ajax({
        type: "POST",
        url: "medicine/delete",
        data: {
            id: id,
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                setTimeout('reload()', 2000);
            }
        }
    });
}

/** 新增模式下暂存的待关联疾病 ID 列表 */
var pendingAssocIllnessIds = [];
/** 新增模式下暂存的待关联疾病名称列表（与 ids 一一对应） */
var pendingAssocIllnessNames = [];

/**
 * 搜索疾病并显示关联候选列表
 */
function searchIllnessForAssoc() {
    var keyword = $('#illness-search').val().trim();
    if (!keyword) {
        layer.msg('请输入疾病名称关键词');
        return;
    }
    $.ajax({
        type: "GET",
        url: "illness/search",
        data: { keyword: keyword },
        dataType: "json",
        success: function (data) {
            if (data['code'] !== 'SUCCESS' || !data['data'] || data['data'].length === 0) {
                $('#illness-search-results').hide().html(
                    '<div style="padding:10px;color:#999;text-align:center;">未找到匹配的疾病</div>'
                ).show();
                return;
            }
            var html = '';
            var alreadyIds = getAssociatedIllnessIds();
            var hasResult = false;
            $.each(data['data'], function (i, illness) {
                if (alreadyIds.indexOf(illness.id) >= 0) {
                    // 已关联的加灰色标记
                    html += '<div style="padding:8px 12px;border-bottom:1px solid #f0f0f0;color:#999;">'
                        + illness.illnessName + ' <span style="font-size:12px;">(已关联)</span></div>';
                } else {
                    hasResult = true;
                    html += '<div class="assoc-result-item" onclick="addIllnessAssoc(' + illness.id + ', \''
                        + illness.illnessName.replace(/'/g, "\\'") + '\')" '
                        + 'style="padding:8px 12px;border-bottom:1px solid #f0f0f0;cursor:pointer;transition:background .2s;" '
                        + 'onmouseover="this.style.background=\'#f0f8ff\'" onmouseout="this.style.background=\'#fff\'">'
                        + illness.illnessName + '</div>';
                }
            });
            if (!hasResult) {
                html += '<div style="padding:10px;color:#999;text-align:center;">所有匹配疾病已关联</div>';
            }
            $('#illness-search-results').html(html).show();
        },
        error: function () {
            layer.msg('搜索失败，请稍后重试');
        }
    });
}

/**
 * 获取当前页面上已关联的疾病 ID 列表
 */
function getAssociatedIllnessIds() {
    var ids = [];
    $('#associated-tags .assoc-tag').each(function () {
        var illnessId = $(this).attr('data-illness-id');
        if (illnessId) {
            var idNum = parseInt(illnessId);
            if (ids.indexOf(idNum) < 0) {
                ids.push(idNum);
            }
        }
    });
    for (var i = 0; i < pendingAssocIllnessIds.length; i++) {
        if (ids.indexOf(pendingAssocIllnessIds[i]) < 0) {
            ids.push(pendingAssocIllnessIds[i]);
        }
    }
    return ids;
}

/**
 * 添加疾病关联
 * @param illnessId 疾病 ID
 * @param illnessName 疾病名称（用于显示）
 */
function addIllnessAssoc(illnessId, illnessName) {
    var medicineId = $('#id').val();
    // --- 新增模式：暂存关联 ---
    if (!medicineId) {
        if (pendingAssocIllnessIds.indexOf(illnessId) >= 0) {
            layer.msg('该疾病已在待关联列表中');
            return;
        }
        pendingAssocIllnessIds.push(illnessId);
        pendingAssocIllnessNames.push(illnessName);
        showAssocTag(illnessId, illnessName, 0, true);
        markSearchResultAssociated(illnessName);
        $('#no-assoc-tip').hide();
        layer.msg('已标记为待关联（保存药品后自动生效）');
        return;
    }
    // --- 编辑模式：直接 AJAX 关联 ---
    $.ajax({
        type: "POST",
        url: "illness_medicine/save",
        data: {
            illnessId: illnessId,
            medicineId: medicineId,
        },
        dataType: "json",
        success: function (data) {
            if (data['code'] === 'SUCCESS') {
                var imeId = data['data'] && data['data'].id ? data['data'].id : 0;
                $('#no-assoc-tip').hide();
                showAssocTag(illnessId, illnessName, imeId, false);
                markSearchResultAssociated(illnessName);
                layer.msg('关联成功');
            } else {
                layer.msg(data['message'] || '关联失败');
            }
        }
    });
}

function showAssocTag(illnessId, illnessName, imeId, pending) {
    var borderColor = pending ? '#f0c060' : '#bee0f5';
    var bgColor = pending ? '#fef9ee' : '#e8f4fd';
    var removeHandler;
    if (pending) {
        removeHandler = 'removePendingAssoc(' + illnessId + ', this)';
    } else {
        removeHandler = 'var tid=$(this).closest(\'.assoc-tag\').attr(\'data-ime-id\');removeIllnessAssoc(tid, this)';
    }
    var tag = $('<span class="assoc-tag" data-illness-id="' + illnessId + '" data-ime-id="' + imeId + '" '
        + (pending ? 'data-pending="1" ' : '')
        + 'style="display:inline-block;margin:3px 5px;padding:4px 10px;background:' + bgColor
        + ';border:1px solid ' + borderColor + ';border-radius:15px;font-size:13px;">'
        + (pending ? '<span style="font-size:11px;color:#e09900;">⏳</span> ' : '')
        + illnessName
        + '<a href="javascript:void(0)" onclick="' + removeHandler + '" '
        + 'style="color:#c00;margin-left:6px;font-weight:bold;text-decoration:none;" title="取消关联">&times;</a>'
        + '</span>');
    $('#associated-tags').append(tag);
}

function markSearchResultAssociated(illnessName) {
    $('#illness-search-results').find('.assoc-result-item').each(function () {
        if ($(this).text().trim() === illnessName) {
            $(this).css('color', '#999').html(illnessName + ' <span style="font-size:12px;">(已关联)</span>')
                .removeClass('assoc-result-item').attr('onclick', '');
        }
    });
}

function removePendingAssoc(illnessId, el) {
    var idx = pendingAssocIllnessIds.indexOf(illnessId);
    if (idx >= 0) {
        pendingAssocIllnessIds.splice(idx, 1);
        pendingAssocIllnessNames.splice(idx, 1);
    }
    $(el).closest('.assoc-tag').remove();
    if ($('#associated-tags .assoc-tag').length === 0) {
        $('#no-assoc-tip').show();
    }
}

function batchCreateAssociations(medicineId, onComplete) {
    if (pendingAssocIllnessIds.length === 0) {
        if (onComplete) onComplete();
        return;
    }
    var total = pendingAssocIllnessIds.length;
    var done = 0;
    function checkDone() {
        done++;
        if (done >= total) {
            pendingAssocIllnessIds = [];
            pendingAssocIllnessNames = [];
            layer.msg('药品已保存，' + total + ' 个疾病关联已创建');
            if (onComplete) onComplete();
        }
    }
    for (var i = 0; i < total; i++) {
        $.ajax({
            type: "POST",
            url: "illness_medicine/save",
            data: {
                illnessId: pendingAssocIllnessIds[i],
                medicineId: medicineId,
            },
            dataType: "json",
            success: checkDone,
            error: checkDone
        });
    }
}

/**
 * 取消疾病关联（已持久化的，编辑模式）
 */
function removeIllnessAssoc(illnessMedicineId, el) {
    $.ajax({
        type: "POST",
        url: "illness_medicine/delete",
        data: { id: illnessMedicineId },
        dataType: "json",
        success: function (data) {
            if (data['code'] === 'SUCCESS') {
                $(el).closest('.assoc-tag').remove();
                if ($('#associated-tags .assoc-tag').length === 0) {
                    $('#no-assoc-tip').show();
                }
                layer.msg('已取消关联');
            } else {
                layer.msg(data['message'] || '操作失败');
            }
        }
    });
}
/**
 * 删除反馈
 * @param id
 */
function deleteFeedback(id) {
    $.ajax({
        type: "POST",
        url: "feedback/delete",
        data: {
            id: id,
        },
        dataType: "json",
        success: function (data) {
            layer.msg(data['message']);
            if (data['code'] === 'SUCCESS') {
                setTimeout('reload()', 2000);
            }
        }
    });
}

/**
 * 初始化聊天窗口滚动条
 */
function messageInit() {
    let height = $("#messages")[0].scrollHeight;
    $("#messages").scrollTop(height);
}

/**
 * 发送消息（流式 SSE，支持停止）
 */
function send() {
    // 停止模式：如果正在流式输出，点击则停止
    if (typeof currentEventSource !== 'undefined' && currentEventSource) {
        stopStream();
        return;
    }

    let message = $('#message').val();
    if (!message) {
        return;
    }

    // 确保有当前会话ID，否则拒绝发送
    if (typeof currentConversationId === 'undefined' || !currentConversationId) {
        layer.msg('会话尚未就绪，请稍后再试', {icon: 2});
        return;
    }

    var timeStr = getTimeStr();
    var displayMessage = textToHtml(message);

    // 追加用户消息（右侧浅灰底）
    $('#messages').append(
        '<div class="msg-sent">' +
            '<div class="msg-content">' + displayMessage + '</div>' +
            '<div class="msg-time">' + timeStr + '</div>' +
        '</div>'
    );
    messageInit();
    $('#message').val('');

    // 创建空的 AI 消息容器
    var msgId = 'stream-' + Date.now();
    $('#messages').append(
        '<div class="msg-received" id="' + msgId + '">' +
            '<div class="msg-image">' +
                '<img src="assets/images/team/user-2.jpg" alt="doctor">' +
            '</div>' +
            '<div class="msg-body">' +
                '<div class="msg-content"><span style="color:#999">思考中…</span></div>' +
            '</div>' +
        '</div>'
    );
    messageInit();

    // EventSource 流式读取（GET 方式，若消息太长则回退到 AJAX）
    var convId = currentConversationId;  // 闭包捕获，供回调使用
    var convParam = '&conversationId=' + encodeURIComponent(convId);
    var streamUrl = 'message/query/stream?content=' + encodeURIComponent(message) + convParam;
    if (streamUrl.length > 1900) {
        // URL 过长 → 回退到 AJAX 非流式
        fallbackAjaxSend(message, msgId);
        return;
    }

    // 初始化缓冲区
    window.streamBuffers[convId] = { text: '', active: true };

    // 切换按钮为停止模式
    $('#sendBtn').text('■ 停止');

    var accumulatedText = '';
    var es = new EventSource(streamUrl);
    currentEventSource = es;

    es.onmessage = function(e) {
        // 无论 UI 是否可见，始终更新缓冲区
        window.streamBuffers[convId].text += e.data;
        // 只在当前会话可见时更新 UI
        if (!es._abandoned) {
            if (accumulatedText === '') {
                $('#' + msgId + ' .msg-content').html('');
            }
            accumulatedText += e.data;
            var html = renderMarkdown(accumulatedText);
            $('#' + msgId + ' .msg-content').html(html);
            messageInit();
        }
    };

    es.addEventListener('done', function() {
        es.close();
        window.streamBuffers[convId].active = false;
        if (currentEventSource === es) currentEventSource = null;
        $('#sendBtn').text('发送');
        // 被遗弃时不再操控 UI，但缓冲区已保留供切回时使用
        if (es._abandoned) return;
        // 从 DB 重新加载消息（获得追问按钮）
        if (typeof reloadCurrentMessages === 'function') {
            reloadCurrentMessages();
        }
        if (typeof loadConversationList === 'function') {
            loadConversationList();
        }
    });

    es.addEventListener('error-event', function(e) {
        es.close();
        window.streamBuffers[convId].active = false;
        if (currentEventSource === es) currentEventSource = null;
        $('#sendBtn').text('发送');
        var errText = e.data || 'AI服务异常';
        $('#' + msgId + ' .msg-content').html('<span style="color:#e74c3c">' + errText + '</span>');
        $('#' + msgId + ' .msg-body').append('<div class="msg-time">' + getTimeStr() + '</div>');
        messageInit();
    });

    es.onerror = function() {
        window.streamBuffers[convId].active = false;
        if (currentEventSource === es) currentEventSource = null;
        if (es._abandoned || es._manualStop) return;
        if (es.readyState === EventSource.CLOSED && !accumulatedText) {
            $('#sendBtn').text('发送');
            $('#' + msgId + ' .msg-content').html('<span style="color:#999">连接已断开</span>');
        }
    };
}

/** 停止当前 AI 流式输出 */
function stopStream() {
    if (currentEventSource) {
        currentEventSource._manualStop = true;
        currentEventSource.close();
        currentEventSource = null;
    }
    $('#sendBtn').text('发送');
    layer.msg('已停止输出', {icon: 0, time: 1000});
}

/** URL 超长时的 AJAX 回退 */
function fallbackAjaxSend(message, msgId) {
    var reqData = { content: message };
    if (typeof currentConversationId !== 'undefined' && currentConversationId) {
        reqData.conversationId = currentConversationId;
    }
    $.ajax({
        type: "POST",
        url: "message/query",
        data: reqData,
        dataType: "json",
        success: function (data) {
            if (data['code'] === 200 || data['code'] === 'SUCCESS') {
                // AJAX 完成后从 DB 重新加载（获得追问按钮）
                if (typeof reloadCurrentMessages === 'function') {
                    reloadCurrentMessages();
                } else {
                    var reply = data['data'] || data['message'];
                    var formattedReply = renderMarkdown(reply);
                    var replyTime = getTimeStr();
                    $('#' + msgId + ' .msg-content').html(formattedReply);
                    $('#' + msgId + ' .msg-body').append('<div class="msg-time">' + replyTime + '</div>');
                }
                if (typeof loadConversationList === 'function') {
                    loadConversationList();
                }
            } else {
                var errMsg = data['msg'] || data['message'] || '请求失败';
                $('#' + msgId + ' .msg-content').html('<span style="color:#e74c3c">' + errMsg + '</span>');
                $('#' + msgId + ' .msg-body').append('<div class="msg-time">' + getTimeStr() + '</div>');
            }
        },
        error: function() {
            $('#' + msgId + ' .msg-content').html('<span style="color:#e74c3c">网络错误，请稍后再试</span>');
            $('#' + msgId + ' .msg-body').append('<div class="msg-time">' + getTimeStr() + '</div>');
            messageInit();
        }
    });
}


/**
 * 搜索病
 */
function searchGroup(kind) {
    let content = $("#search").val().trim();
    if (content == ""){
        xtip.msg("请输入查询内容");
        return;
    }
    let href = window.location.href;
    href = href.split("/")[0] + "/findIllness?illnessName="+content+"&kind="+kind;
    reloadToGO(href);
}

/**
 * 搜索病
 */
function searchGroupByName() {
    let content = $("#search").val().trim();
    if (content == ""){
        xtip.msg("请输入查询内容");
        return;
    }
    let href = window.location.href;
    href = href.split("/")[0] + "/findIllness?illnessName="+content;
    reloadToGO(href);
}

/**
 * 搜索一下
 */
function searchGlobalSelect() {
    let content = $("#cf-search-form").val().trim();
    if (content == ""){
        xtip.msg("请输入查询内容");
        return;
    }
    let href = window.location.href;
    alert(href);
    href = href.split("/")[0] + "/findIllness?illnessName="+content;
    reloadToGO(href);
}
/**
 * 搜索药
 */
function searchMedicine() {
    let content = $("#search-medicine").val().trim();
    if (content == ""){
        xtip.msg("请输入查询内容");
        return;
    }
    let href = window.location.href;
    href = href.split("/")[0] + "/findMedicines?nameValue="+content;
    reloadToGO(href);
}

