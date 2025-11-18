// 码替换功能 JavaScript

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('码替换页面初始化');
    
    // 初始化时间显示
    updateCurrentTime();
    setInterval(updateCurrentTime, 1000);
    
    // 初始化统计数据
    updateStatistics();
    
    // 绑定输入框事件
    bindInputEvents();
});

// 更新当前时间
function updateCurrentTime() {
    const now = new Date();
    const timeString = now.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
    
    const timeElement = document.getElementById('currentTime');
    if (timeElement) {
        timeElement.textContent = timeString;
    }
}

// 更新统计数据
function updateStatistics() {
    // 模拟统计数据
    const stats = {
        todayCount: 5,
        totalCount: 128,
        successRate: 96.8
    };
    
    const todayCountElement = document.getElementById('todayCount');
    const totalCountElement = document.getElementById('totalCount');
    const successRateElement = document.getElementById('successRate');
    
    if (todayCountElement) todayCountElement.textContent = stats.todayCount;
    if (totalCountElement) totalCountElement.textContent = stats.totalCount;
    if (successRateElement) successRateElement.textContent = stats.successRate;
}

// 绑定输入框事件
function bindInputEvents() {
    const oldCodeValue = document.getElementById('oldCodeValue');
    const newCodeValue = document.getElementById('newCodeValue');
    const replaceReason = document.getElementById('replaceReason');
    
    if (oldCodeValue) {
        oldCodeValue.addEventListener('input', checkReplaceButtonState);
    }
    
    if (newCodeValue) {
        newCodeValue.addEventListener('input', checkReplaceButtonState);
    }
    
    if (replaceReason) {
        replaceReason.addEventListener('input', checkReplaceButtonState);
    }
}

// 检查替换按钮状态
function checkReplaceButtonState() {
    const oldCodeValue = document.getElementById('oldCodeValue');
    const newCodeValue = document.getElementById('newCodeValue');
    const replaceBtn = document.getElementById('replaceBtn');
    
    if (oldCodeValue && newCodeValue && replaceBtn) {
        const canReplace = oldCodeValue.value.trim() && newCodeValue.value.trim();
        replaceBtn.disabled = !canReplace;
    }
}

// 确认码替换
function confirmCodeReplace() {
    const oldCodeValue = document.getElementById('oldCodeValue');
    const newCodeValue = document.getElementById('newCodeValue');
    const replaceReason = document.getElementById('replaceReason');
    
    if (!oldCodeValue || !oldCodeValue.value.trim()) {
        Message.warning('请输入原码值');
        return;
    }
    
    if (!newCodeValue || !newCodeValue.value.trim()) {
        Message.warning('请输入新码值');
        return;
    }
    
    if (oldCodeValue.value.trim() === newCodeValue.value.trim()) {
        Message.warning('新码值不能与原码值相同');
        return;
    }
    
    // 显示确认对话框
    showConfirmDialog(oldCodeValue.value.trim(), newCodeValue.value.trim(), replaceReason.value.trim());
}

// 显示确认对话框
function showConfirmDialog(oldCode, newCode, reason) {
    const confirmDialog = document.getElementById('confirmDialog');
    const confirmOldCode = document.getElementById('confirmOldCode');
    const confirmNewCode = document.getElementById('confirmNewCode');
    const confirmReason = document.getElementById('confirmReason');
    
    if (confirmOldCode) confirmOldCode.textContent = oldCode;
    if (confirmNewCode) confirmNewCode.textContent = newCode;
    if (confirmReason) confirmReason.textContent = reason || '无';
    
    if (confirmDialog) {
        confirmDialog.style.display = 'flex';
    }
}

// 关闭确认对话框
function closeConfirmDialog() {
    const confirmDialog = document.getElementById('confirmDialog');
    if (confirmDialog) {
        confirmDialog.style.display = 'none';
    }
}

// 执行替换
function executeReplace() {
    const confirmOldCode = document.getElementById('confirmOldCode');
    const confirmNewCode = document.getElementById('confirmNewCode');
    const confirmReason = document.getElementById('confirmReason');
    
    if (!confirmOldCode || !confirmNewCode) return;
    
    const oldCode = confirmOldCode.textContent;
    const newCode = confirmNewCode.textContent;
    const reason = confirmReason.textContent;
    
    // 关闭确认对话框
    closeConfirmDialog();
    
    // 显示处理中状态
    Message.info('正在执行码替换...');
    
    // 模拟替换过程
    setTimeout(() => {
        // 模拟成功
        const success = Math.random() > 0.1; // 90% 成功率
        
        if (success) {
            Message.success('码替换成功！');
            
            // 清空表单
            clearForm();
            
            // 更新统计
            updateStatistics();
        } else {
            // 显示错误
            showErrorDialog('替换失败', '系统繁忙，请稍后重试');
        }
    }, 2000);
}

// 显示错误对话框
function showErrorDialog(title, message, details = '') {
    const errorDialog = document.getElementById('errorDialog');
    const errorMessage = document.getElementById('errorMessage');
    const errorDetails = document.getElementById('errorDetails');
    
    if (errorMessage) errorMessage.textContent = message;
    if (errorDetails) errorDetails.textContent = details;
    
    if (errorDialog) {
        errorDialog.style.display = 'flex';
    }
}

// 关闭错误对话框
function closeErrorDialog() {
    const errorDialog = document.getElementById('errorDialog');
    if (errorDialog) {
        errorDialog.style.display = 'none';
    }
}

// 重试替换
function retryReplace() {
    closeErrorDialog();
    confirmCodeReplace();
}

// 清空表单
function clearForm() {
    const oldCodeValue = document.getElementById('oldCodeValue');
    const newCodeValue = document.getElementById('newCodeValue');
    const replaceReason = document.getElementById('replaceReason');
    
    if (oldCodeValue) oldCodeValue.value = '';
    if (newCodeValue) newCodeValue.value = '';
    if (replaceReason) replaceReason.value = '';
    
    checkReplaceButtonState();
}

// 点击对话框外部关闭对话框
document.addEventListener('click', function(event) {
    const confirmDialog = document.getElementById('confirmDialog');
    const errorDialog = document.getElementById('errorDialog');
    
    if (event.target === confirmDialog) {
        closeConfirmDialog();
    }
    
    if (event.target === errorDialog) {
        closeErrorDialog();
    }
}); 