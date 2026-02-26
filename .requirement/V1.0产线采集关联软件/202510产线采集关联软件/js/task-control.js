// 任务执行控制页面JavaScript功能 - 工控机触摸屏优化

// 全局变量
let currentTask = null;
let taskStatus = 'pending'; // pending, running, completed, stopped
let statusUpdateTimer = null;
let runningTimer = null;
let startTime = null;
let operationLogs = [];
let singleCodeRecords = []; // 单码采集记录
let packageRelationRecords = []; // 包装关联记录
let alarmRecords = []; // 报警记录
let currentTab = 'single-code'; // 当前激活的Tab
let deviceStatus = {
    scanner: 'online',
    printer: 'online',
    database: 'online'
};
let confirmAction = null;
let emergencyCountdownTimer = null;
let emergencyCountdownValue = 3;

// 新增状态变量
let workingStatus = 'standby'; // standby, working, paused
let removeFeatureEnabled = false; // 剔除功能是否开启

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    initializePage();
});

function initializePage() {
    // 更新时间显示
    updateTime();
    setInterval(updateTime, 1000);
    
    // 初始化任务数据
    initializeTaskData();
    
    // 更新界面状态
    updateTaskInfo();
    updateStatusIndicator();
    updateControlButtons();
    updateDeviceStatus();
    updateWorkingStatus();
    updateRemoveStatus();
    
    // 启动状态监控
    startStatusMonitoring();
    
    // 添加操作日志
    addOperationLog('系统初始化完成');
    
    // 显示欢迎消息
    setTimeout(() => {
        showMessage('任务执行控制界面加载完成', 'info');
    }, 500);
}

function updateTime() {
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

// 初始化任务数据
function initializeTaskData() {
    // 从URL参数获取任务ID，或使用默认任务
    const urlParams = new URLSearchParams(window.location.search);
    const taskId = urlParams.get('taskId');
    
    // 模拟任务数据
    currentTask = {
        id: taskId || 1,
        productionOrder: 'T20241201001',
        productCode: 'P001',
        productName: '维生素C片',
        plannedQty: 5000,
        productionBatch: 'B20241201001',
        packageRatio: '1:12:100',
        measureUnit: '瓶',
        productionDate: '2024-12-01',
        expiryDate: '2026-11-30',
        totalInspected: 0,
        qualifiedCount: 0,
        unqualifiedCount: 0,
        packageTotal: 0,
        progress: 0,
        status: 'pending',
        boxesPerPallet: 12,
        currentBoxes: 0,
        currentPallets: 0,
        completionRate: 0
    };
    
    taskStatus = currentTask.status;
    
    // 初始化数据记录
    initializeDataRecords();
}

// 初始化数据记录
function initializeDataRecords() {
    singleCodeRecords = [];
    packageRelationRecords = [];
    alarmRecords = [];
}

// 更新任务信息显示
function updateTaskInfo() {
    if (!currentTask) return;
    
    // 更新生产信息
    const productionOrderInput = document.getElementById('productionOrderInput');
    if (productionOrderInput) {
        productionOrderInput.value = currentTask.productionOrder;
    }
    const productCode = document.getElementById('productCode');
    if (productCode) {
        productCode.textContent = currentTask.productCode;
    }
    
    const productName = document.getElementById('productName');
    if (productName) {
        productName.textContent = currentTask.productName;
    }
    
    const plannedQty = document.getElementById('plannedQty');
    if (plannedQty) {
        plannedQty.textContent = currentTask.plannedQty.toLocaleString();
    }
    
    const productionBatch = document.getElementById('productionBatch');
    if (productionBatch) {
        productionBatch.value = currentTask.productionBatch;
    }
    
    const packageRatio = document.getElementById('packageRatio');
    if (packageRatio) {
        packageRatio.textContent = currentTask.packageRatio;
    }
    const measureUnit = document.getElementById('measureUnit');
    if (measureUnit) {
        measureUnit.textContent = currentTask.measureUnit;
    }
    
    const productionDate = document.getElementById('productionDate');
    if (productionDate) {
        productionDate.textContent = currentTask.productionDate;
    }
    
    const expiryDate = document.getElementById('expiryDate');
    if (expiryDate) {
        expiryDate.textContent = currentTask.expiryDate;
    }
    
    // 更新即时统计数据
    document.getElementById('totalInspected').textContent = String(currentTask.totalInspected).padStart(9, '0');
    document.getElementById('qualifiedCount').textContent = String(currentTask.qualifiedCount).padStart(9, '0');
    document.getElementById('unqualifiedCount').textContent = String(currentTask.unqualifiedCount).padStart(9, '0');
    document.getElementById('packageTotal').textContent = String(currentTask.packageTotal).padStart(9, '0');
    

    
    // 更新单位实时统计
    document.getElementById('boxesPerPallet').textContent = currentTask.boxesPerPallet;
    document.getElementById('currentBoxes').textContent = currentTask.currentBoxes;
    document.getElementById('currentPallets').textContent = currentTask.currentPallets;
    document.getElementById('completionRate').textContent = `${currentTask.completionRate}%`;
}

// 更新状态指示器
function updateStatusIndicator() {
    const statusLight = document.getElementById('statusLight');
    const statusIcon = document.getElementById('statusIcon');
    const statusText = document.getElementById('statusText');
    
    // 清除所有状态类
    statusLight.className = 'status-light';
    
    switch (taskStatus) {
        case 'pending':
            statusLight.classList.add('pending');
            statusIcon.textContent = '⏸️';
            statusText.textContent = '待开始';
            break;
        case 'running':
            statusLight.classList.add('running');
            statusIcon.textContent = '▶️';
            statusText.textContent = '运行中';
            break;
        case 'paused':
            statusLight.classList.add('paused');
            statusIcon.textContent = '⏸️';
            statusText.textContent = '已暂停';
            break;
        case 'completed':
            statusLight.classList.add('completed');
            statusIcon.textContent = '✅';
            statusText.textContent = '已完成';
            break;
        case 'stopped':
            statusLight.classList.add('stopped');
            statusIcon.textContent = '⏹️';
            statusText.textContent = '已停止';
            break;
        case 'error':
            statusLight.classList.add('error');
            statusIcon.textContent = '⚠️';
            statusText.textContent = '异常状态';
            break;
    }
    
    // 状态栏中的任务状态已移除，工作状态已单独显示
    
    // 更新操作提示
    updateOperationTips();
}

// 更新操作提示
function updateOperationTips() {
    const tipsContent = document.getElementById('tipsContent');
    
    switch (taskStatus) {
        case 'pending':
            tipsContent.textContent = '点击启动按钮开始生产任务';
            break;
        case 'running':
            tipsContent.textContent = '任务正在执行中，可点击暂停或停止';
            break;
        case 'paused':
            tipsContent.textContent = '任务已暂停，可点击继续或停止';
            break;
        case 'completed':
            tipsContent.textContent = '任务已完成，可查看执行结果';
            break;
        case 'stopped':
            tipsContent.textContent = '任务已停止，可重新启动或选择新任务';
            break;
        case 'error':
            tipsContent.textContent = '检测到异常，请检查设备连接状态';
            break;
    }
}

// 更新控制按钮状态
function updateControlButtons() {
    // 更新切换按钮状态
    updateToggleButton();
    
    // 更新完成订单按钮状态
    updateCompleteOrderButton();
    
    // 保留原有的暂停/继续按钮逻辑（如果存在）
    const pauseBtn = document.getElementById('pauseBtn');
    const continueBtn = document.getElementById('continueBtn');
    
    if (pauseBtn && continueBtn) {
        // 重置暂停/继续按钮状态
        pauseBtn.disabled = true;
        continueBtn.disabled = true;
        
        // 隐藏继续按钮
        continueBtn.style.display = 'none';
        pauseBtn.style.display = 'flex';
        
        switch (taskStatus) {
            case 'running':
                pauseBtn.disabled = false;
                break;
            case 'paused':
                continueBtn.disabled = false;
                // 显示继续按钮，隐藏暂停按钮
                continueBtn.style.display = 'flex';
                pauseBtn.style.display = 'none';
                break;
        }
    }
}

// 更新设备状态
function updateDeviceStatus() {
    const scannerStatusElement = document.getElementById('scannerStatus');
    const printerStatusElement = document.getElementById('printerStatus');
    const databaseStatusElement = document.getElementById('databaseStatus');
    
    if (scannerStatusElement) {
        scannerStatusElement.textContent = deviceStatus.scanner === 'online' ? '在线' : '离线';
        scannerStatusElement.className = `device-status-indicator ${deviceStatus.scanner}`;
    }
    
    if (printerStatusElement) {
        printerStatusElement.textContent = deviceStatus.printer === 'online' ? '在线' : '离线';
        printerStatusElement.className = `device-status-indicator ${deviceStatus.printer}`;
    }
    
    if (databaseStatusElement) {
        databaseStatusElement.textContent = deviceStatus.database === 'online' ? '连接' : '断开';
        databaseStatusElement.className = `device-status-indicator ${deviceStatus.database}`;
    }
    
    // 更新连接状态
    const allOnline = Object.values(deviceStatus).every(status => status === 'online');
    document.getElementById('connectionStatus').textContent = allOnline ? '设备已连接' : '设备异常';
}

// 更新工作状态
function updateWorkingStatus() {
    const workingStatusElement = document.getElementById('workingStatus');
    if (!workingStatusElement) return;
    
    // 清除所有状态类
    workingStatusElement.className = 'status-indicator';
    
    switch (workingStatus) {
        case 'standby':
            workingStatusElement.classList.add('standby');
            workingStatusElement.textContent = '待机中';
            break;
        case 'working':
            workingStatusElement.classList.add('working');
            workingStatusElement.textContent = '工作中';
            break;
        case 'paused':
            workingStatusElement.classList.add('paused');
            workingStatusElement.textContent = '已暂停';
            break;
    }
}

// 更新剔除功能状态
function updateRemoveStatus() {
    const removeStatusElement = document.getElementById('removeStatus');
    if (!removeStatusElement) return;
    
    // 清除所有状态类
    removeStatusElement.className = 'status-indicator';
    
    if (removeFeatureEnabled) {
        removeStatusElement.classList.add('enabled');
        removeStatusElement.textContent = '已开启';
    } else {
        removeStatusElement.classList.add('disabled');
        removeStatusElement.textContent = '已关闭';
    }
}

// 启动任务
function startTask() {
    if (!checkDeviceStatus()) {
        showMessage('设备离线，无法启动任务', 'error');
        return;
    }
    
    const confirmDetails = `
        <div><strong>任务单号：</strong>${currentTask.taskNumber}</div>
        <div><strong>产品名称：</strong>${currentTask.productName}</div>
        <div><strong>计划数量：</strong>${currentTask.plannedQty.toLocaleString()}</div>
        <div><strong>采集模式：</strong>${currentTask.collectionMode}</div>
    `;
    
    showConfirmDialog(
        '启动任务确认',
        '您确定要启动此生产任务吗？',
        confirmDetails,
        '🚀',
        () => executeStartTask()
    );
}

function executeStartTask() {
    taskStatus = 'running';
    workingStatus = 'working';
    startTime = new Date();
    currentTask.status = 'running';
    
    updateTaskInfo();
    updateStatusIndicator();
    updateControlButtons();
    updateWorkingStatus();
    
    // 启动运行时间计时器
    startRunningTimer();
    
    // 模拟生产进度
    startProductionSimulation();
    
    addOperationLog('启动任务', '成功');
    showMessage('任务启动成功', 'success');
    
    // 播放启动音效
    playSound('start');
}

// 暂停任务
function pauseTask() {
    showConfirmDialog(
        '暂停任务确认',
        '您确定要暂停当前任务吗？',
        '暂停后可以继续执行任务',
        '⏸️',
        () => executePauseTask()
    );
}

function executePauseTask() {
    taskStatus = 'paused';
    workingStatus = 'paused';
    currentTask.status = 'paused';
    
    updateStatusIndicator();
    updateControlButtons();
    updateWorkingStatus();
    
    // 停止运行时间计时器
    stopRunningTimer();
    
    // 停止生产模拟
    stopProductionSimulation();
    
    addOperationLog('暂停任务', '成功');
    showMessage('任务已暂停', 'warning');
    
    playSound('pause');
}

// 继续任务
function continueTask() {
    if (!checkDeviceStatus()) {
        showMessage('设备离线，无法继续任务', 'error');
        return;
    }
    
    taskStatus = 'running';
    workingStatus = 'working';
    currentTask.status = 'running';
    
    updateStatusIndicator();
    updateControlButtons();
    updateWorkingStatus();
    
    // 重新启动运行时间计时器
    startRunningTimer();
    
    // 重新启动生产模拟
    startProductionSimulation();
    
    addOperationLog('继续任务', '成功');
    showMessage('任务继续执行', 'success');
    
    playSound('start');
}

// 停止任务
function stopTask() {
    showConfirmDialog(
        '停止任务确认',
        '您确定要停止当前任务吗？',
        '停止后需要重新启动任务',
        '⏹️',
        () => executeStopTask()
    );
}

function executeStopTask() {
    taskStatus = 'stopped';
    workingStatus = 'standby';
    currentTask.status = 'stopped';
    
    updateStatusIndicator();
    updateControlButtons();
    updateWorkingStatus();
    
    // 停止所有计时器
    stopRunningTimer();
    stopProductionSimulation();
    
    addOperationLog('停止任务', '成功');
    showMessage('任务已停止', 'warning');
    
    playSound('stop');
}

// 紧急停止
function emergencyStop() {
    showEmergencyDialog();
}

function executeEmergencyStop() {
    taskStatus = 'stopped';
    currentTask.status = 'stopped';
    
    // 立即停止所有操作
    stopRunningTimer();
    stopProductionSimulation();
    
    updateStatusIndicator();
    updateControlButtons();
    
    addOperationLog('紧急停止', '成功', '操作员执行紧急停止');
    showMessage('紧急停止执行成功', 'error');
    
    // 播放紧急停止音效
    playSound('emergency');
    
    closeEmergencyDialog();
}

// 运行时间计时器
function startRunningTimer() {
    if (runningTimer) {
        clearInterval(runningTimer);
    }
    
    runningTimer = setInterval(() => {
        if (startTime) {
            const runningTime = Date.now() - startTime;
            document.getElementById('runningTime').textContent = formatDuration(runningTime);
        }
    }, 1000);
}

function stopRunningTimer() {
    if (runningTimer) {
        clearInterval(runningTimer);
        runningTimer = null;
    }
}

// 生产进度模拟
let productionTimer = null;

// 数据表格管理
function refreshDataTable() {
    showMessage('正在刷新数据表格...', 'info');
    
    setTimeout(() => {
        updateDataTable();
        showMessage('数据表格已刷新', 'success');
    }, 1000);
}

function updateDataTable() {
    const tbody = document.getElementById('dataTableBody');
    if (!tbody) return;
    
    // 清空现有数据
    tbody.innerHTML = '';
    
    // 显示最近的数据记录（最多显示100条）
    const recentRecords = dataRecords.slice(-100).reverse();
    
    recentRecords.forEach((record, index) => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${recentRecords.length - index}</td>
            <td>${record.productCode}</td>
            <td>${record.packageCode}</td>
            <td>${record.collectTime}</td>
            <td><span class="status-tag ${record.status === '成功' ? 'completed' : 'error'}">${record.status}</span></td>
            <td>${record.note || ''}</td>
        `;
        tbody.appendChild(row);
    });
}

function exportData() {
    showMessage('正在导出数据...', 'info');
    
    setTimeout(() => {
        // 模拟数据导出
        const csvContent = generateCSVContent();
        downloadCSV(csvContent, `数据采集记录_${currentTask.taskNumber}_${new Date().toISOString().split('T')[0]}.csv`);
        showMessage('数据导出完成', 'success');
    }, 2000);
}

function generateCSVContent() {
    let csv = '序号,产品编码,包装编码,采集时间,状态,备注\n';
    
    dataRecords.forEach((record, index) => {
        csv += `${index + 1},"${record.productCode}","${record.packageCode}","${record.collectTime}","${record.status}","${record.note || ''}"\n`;
    });
    
    return csv;
}

function downloadCSV(content, filename) {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    
    if (link.download !== undefined) {
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', filename);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}

// 模拟数据采集
function simulateDataCollection() {
    if (taskStatus !== 'running') return;
    
    // 生成模拟数据记录
    const record = {
        productCode: `${currentTask.productCode}${String(currentTask.totalQty + 1).padStart(6, '0')}`,
        packageCode: generatePackageCode(),
        collectTime: new Date().toLocaleString('zh-CN'),
        status: Math.random() > 0.02 ? '成功' : '失败', // 98% 成功率
        note: ''
    };
    
    // 添加到记录数组
    dataRecords.push(record);
    
    // 更新统计数据
    currentTask.totalQty++;
    if (record.status === '成功') {
        currentTask.completedQty++;
    } else {
        currentTask.defectiveQty++;
        record.note = '质量检测不合格';
    }
    
    // 更新进度
    currentTask.progress = Math.floor((currentTask.completedQty / currentTask.plannedQty) * 100);
    
    // 更新界面
    updateTaskInfo();
    updateDataTable();
    
    // 添加操作日志
    if (record.status === '失败') {
        addOperationLog('数据采集', '警告', `检测到不合格产品: ${record.productCode}`);
    }
}

function generatePackageCode() {
    const prefix = 'PKG';
    const timestamp = Date.now().toString().slice(-8);
    const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    return `${prefix}${timestamp}${random}`;
}

// 日志管理
function clearLog() {
    if (confirm('确定要清空操作日志吗？')) {
        const logList = document.getElementById('operationLogList');
        if (logList) {
            logList.innerHTML = '<div class="log-item"><span class="log-time">' + 
                new Date().toLocaleString() + 
                '</span><span class="log-content">日志已清空</span></div>';
        }
        
        // 清空日志数组，但保留最后一条
        operationLogs = operationLogs.slice(-1);
        addOperationLog('清空日志', '成功');
        
        showMessage('操作日志已清空', 'success');
    }
}

// 重写生产进度模拟函数，集成数据采集
function startProductionSimulation() {
    if (productionTimer) {
        clearInterval(productionTimer);
    }
    
    productionTimer = setInterval(() => {
        if (taskStatus === 'running') {
            // 模拟数据采集（每2秒采集1-3个产品）
            const collectCount = Math.floor(Math.random() * 3) + 1;
            
            for (let i = 0; i < collectCount; i++) {
                if (currentTask.completedQty < currentTask.plannedQty) {
                    simulateDataCollection();
                }
            }
            
            // 检查是否完成
            if (currentTask.completedQty >= currentTask.plannedQty) {
                completeTask();
            }
            
            // 模拟偶发异常
            if (Math.random() < 0.001) { // 0.1% 概率
                simulateError();
            }
        }
    }, 2000); // 每2秒更新一次
}

function stopProductionSimulation() {
    if (productionTimer) {
        clearInterval(productionTimer);
        productionTimer = null;
    }
}

function completeTask() {
    taskStatus = 'completed';
    currentTask.status = 'completed';
    currentTask.progress = 100;
    
    stopProductionSimulation();
    stopRunningTimer();
    
    updateTaskInfo();
    updateStatusIndicator();
    updateControlButtons();
    
    addOperationLog('任务完成', '成功');
    showMessage('任务执行完成！', 'success');
    
    // 播放完成音效
    playSound('complete');
}

function simulateError() {
    taskStatus = 'error';
    currentTask.status = 'error';
    
    updateStatusIndicator();
    updateControlButtons();
    
    // 模拟设备异常
    if (Math.random() < 0.5) {
        deviceStatus.scanner = 'offline';
    } else {
        deviceStatus.printer = 'warning';
    }
    
    updateDeviceStatus();
    
    addOperationLog('检测到异常', '警告', '设备连接异常');
    showMessage('检测到设备异常，请检查设备状态', 'error');
    
    playSound('error');
    
    // 5秒后自动恢复（模拟）
    setTimeout(() => {
        deviceStatus.scanner = 'online';
        deviceStatus.printer = 'online';
        updateDeviceStatus();
        
        if (taskStatus === 'error') {
            taskStatus = 'paused';
            currentTask.status = 'paused';
            updateStatusIndicator();
            updateControlButtons();
            addOperationLog('异常已恢复', '信息', '设备连接已恢复');
        }
    }, 5000);
}

// 状态监控
function startStatusMonitoring() {
    if (statusUpdateTimer) {
        clearInterval(statusUpdateTimer);
    }
    
    statusUpdateTimer = setInterval(() => {
        // 模拟状态检查
        updateDeviceStatus();
        
        // 检查网络连接
        if (!navigator.onLine) {
            showMessage('网络连接异常', 'warning');
        }
    }, 2000); // 每2秒检查一次
}

// 辅助函数
function checkDeviceStatus() {
    return Object.values(deviceStatus).every(status => status === 'online');
}

function calculateCurrentSpeed() {
    if (!startTime || taskStatus !== 'running') {
        return 0;
    }
    
    const runningMinutes = (Date.now() - startTime) / (1000 * 60);
    if (runningMinutes === 0) return 0;
    
    return Math.floor(currentTask.completedQty / runningMinutes);
}

function formatTime(date) {
    if (!date) return '--:--:--';
    return date.toTimeString().split(' ')[0].substring(0, 8);
}

function formatDuration(milliseconds) {
    const seconds = Math.floor(milliseconds / 1000);
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

// 操作日志
function addOperationLog(operation, result = '成功', note = '') {
    const now = new Date();
    const timeString = now.toTimeString().split(' ')[0].substring(0, 8);
    
    const logEntry = {
        time: timeString,
        operator: '操作员01',
        operation: operation,
        result: result,
        note: note,
        timestamp: now
    };
    
    operationLogs.unshift(logEntry);
    
    // 只保留最近50条记录
    if (operationLogs.length > 50) {
        operationLogs = operationLogs.slice(0, 50);
    }
    
    // 更新显示的最近操作
    updateRecentOperations();
}

function updateRecentOperations() {
    const logList = document.getElementById('operationLogList');
    if (!logList) return;
    
    // 显示最近5条操作
    const recentLogs = operationLogs.slice(0, 5);
    
    logList.innerHTML = recentLogs.map(log => `
        <div class="log-item">
            <span class="log-time">${log.time}</span>
            <span class="log-content">${log.operation} - ${log.result}</span>
        </div>
    `).join('');
}

// 确认对话框
function showConfirmDialog(title, message, details, icon, callback) {
    const dialog = document.getElementById('confirmDialog');
    const titleElement = document.getElementById('confirmTitle');
    const messageElement = document.getElementById('confirmMessage');
    const detailsElement = document.getElementById('confirmDetails');
    const iconElement = document.getElementById('confirmIcon');
    
    titleElement.textContent = title;
    messageElement.textContent = message;
    detailsElement.innerHTML = details;
    iconElement.textContent = icon;
    
    confirmAction = callback;
    dialog.style.display = 'flex';
}

function closeConfirmDialog() {
    const dialog = document.getElementById('confirmDialog');
    dialog.style.display = 'none';
    confirmAction = null;
}

function executeConfirmedAction() {
    if (confirmAction) {
        confirmAction();
        closeConfirmDialog();
    }
}

// 紧急停止对话框
function showEmergencyDialog() {
    const dialog = document.getElementById('emergencyDialog');
    dialog.style.display = 'flex';
    
    // 重置倒计时
    emergencyCountdownValue = 3;
    document.getElementById('countdownTimer').textContent = emergencyCountdownValue;
    document.getElementById('countdownProgress').style.width = '0%';
}

function closeEmergencyDialog() {
    const dialog = document.getElementById('emergencyDialog');
    dialog.style.display = 'none';
    
    // 清除倒计时
    if (emergencyCountdownTimer) {
        clearInterval(emergencyCountdownTimer);
        emergencyCountdownTimer = null;
    }
}

function startEmergencyCountdown() {
    if (emergencyCountdownTimer) return;
    
    emergencyCountdownValue = 3;
    let progress = 0;
    
    emergencyCountdownTimer = setInterval(() => {
        progress += 100 / 30; // 3秒 = 30个100ms
        document.getElementById('countdownProgress').style.width = `${progress}%`;
        
        const remainingTime = Math.ceil((30 - (progress / (100/30))) / 10);
        document.getElementById('countdownTimer').textContent = remainingTime;
        
        if (progress >= 100) {
            executeEmergencyStop();
            clearInterval(emergencyCountdownTimer);
            emergencyCountdownTimer = null;
        }
    }, 100);
}

function stopEmergencyCountdown() {
    if (emergencyCountdownTimer) {
        clearInterval(emergencyCountdownTimer);
        emergencyCountdownTimer = null;
        
        // 重置进度条
        document.getElementById('countdownProgress').style.width = '0%';
        document.getElementById('countdownTimer').textContent = '3';
    }
}

// 设备状态对话框
function showDeviceStatus() {
    const dialog = document.getElementById('deviceStatusDialog');
    const content = document.getElementById('deviceStatusContent');
    
    content.innerHTML = createDeviceStatusContent();
    dialog.style.display = 'flex';
}

function createDeviceStatusContent() {
    return `
        <div class="device-detail-grid">
            <div class="device-detail-card">
                <h4>扫码器状态</h4>
                <div class="device-detail-item">
                    <span class="device-detail-label">连接状态：</span>
                    <span class="device-detail-value">${deviceStatus.scanner === 'online' ? '在线' : '离线'}</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">设备型号：</span>
                    <span class="device-detail-value">Scanner-2000</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">IP地址：</span>
                    <span class="device-detail-value">192.168.1.100</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">最后通信：</span>
                    <span class="device-detail-value">${new Date().toLocaleTimeString()}</span>
                </div>
            </div>
            
            <div class="device-detail-card">
                <h4>打印机状态</h4>
                <div class="device-detail-item">
                    <span class="device-detail-label">连接状态：</span>
                    <span class="device-detail-value">${deviceStatus.printer === 'online' ? '在线' : '离线'}</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">设备型号：</span>
                    <span class="device-detail-value">Printer-3000</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">纸张状态：</span>
                    <span class="device-detail-value">正常</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">墨盒状态：</span>
                    <span class="device-detail-value">充足</span>
                </div>
            </div>
            
            <div class="device-detail-card">
                <h4>数据库连接</h4>
                <div class="device-detail-item">
                    <span class="device-detail-label">连接状态：</span>
                    <span class="device-detail-value">${deviceStatus.database === 'online' ? '已连接' : '断开'}</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">服务器：</span>
                    <span class="device-detail-value">192.168.1.10</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">数据库：</span>
                    <span class="device-detail-value">ProductionDB</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">响应时间：</span>
                    <span class="device-detail-value">< 100ms</span>
                </div>
            </div>
            
            <div class="device-detail-card">
                <h4>系统状态</h4>
                <div class="device-detail-item">
                    <span class="device-detail-label">CPU使用率：</span>
                    <span class="device-detail-value">25%</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">内存使用：</span>
                    <span class="device-detail-value">60%</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">磁盘空间：</span>
                    <span class="device-detail-value">75%</span>
                </div>
                <div class="device-detail-item">
                    <span class="device-detail-label">网络状态：</span>
                    <span class="device-detail-value">正常</span>
                </div>
            </div>
        </div>
    `;
}

function closeDeviceStatusDialog() {
    const dialog = document.getElementById('deviceStatusDialog');
    dialog.style.display = 'none';
}

function refreshDeviceStatus() {
    // 模拟刷新设备状态
    showMessage('正在刷新设备状态...', 'info');
    
    setTimeout(() => {
        // 随机更新设备状态
        if (Math.random() < 0.1) {
            deviceStatus.scanner = deviceStatus.scanner === 'online' ? 'offline' : 'online';
        }
        
        updateDeviceStatus();
        
        // 更新对话框内容
        const content = document.getElementById('deviceStatusContent');
        content.innerHTML = createDeviceStatusContent();
        
        showMessage('设备状态已刷新', 'success');
    }, 1000);
}

// 操作日志对话框
function showOperationLog() {
    const dialog = document.getElementById('operationLogDialog');
    
    // 设置默认日期范围
    const today = new Date();
    const startDate = new Date(today);
    startDate.setDate(today.getDate() - 7); // 最近7天
    
    document.getElementById('logStartDate').value = startDate.toISOString().split('T')[0];
    document.getElementById('logEndDate').value = today.toISOString().split('T')[0];
    
    // 加载操作日志
    loadOperationLogTable();
    
    dialog.style.display = 'flex';
}

function loadOperationLogTable() {
    const tbody = document.getElementById('operationLogTableBody');
    
    tbody.innerHTML = operationLogs.map(log => `
        <tr>
            <td>${log.timestamp.toLocaleString()}</td>
            <td>${log.operator}</td>
            <td>${log.operation}</td>
            <td><span class="status-tag ${log.result === '成功' ? 'completed' : log.result === '警告' ? 'paused' : 'error'}">${log.result}</span></td>
            <td>${log.note}</td>
        </tr>
    `).join('');
}

function filterOperationLog() {
    // 实现日期筛选逻辑
    showMessage('日志筛选功能开发中...', 'info');
}

function exportOperationLog() {
    // 实现日志导出功能
    showMessage('正在导出操作日志...', 'info');
    
    setTimeout(() => {
        showMessage('操作日志导出完成', 'success');
    }, 2000);
}

function closeOperationLogDialog() {
    const dialog = document.getElementById('operationLogDialog');
    dialog.style.display = 'none';
}

// 帮助对话框
function showHelp() {
    const dialog = document.getElementById('helpDialog');
    dialog.style.display = 'flex';
}

function closeHelpDialog() {
    const dialog = document.getElementById('helpDialog');
    dialog.style.display = 'none';
}

// 刷新状态
function refreshStatus() {
    showMessage('正在刷新任务状态...', 'info');
    
    // 模拟状态刷新
    setTimeout(() => {
        updateTaskInfo();
        updateStatusIndicator();
        updateControlButtons();
        updateDeviceStatus();
        
        addOperationLog('刷新状态', '成功');
        showMessage('任务状态已刷新', 'success');
    }, 1000);
}

// 音效播放
function playSound(type) {
    // 模拟音效播放
    if (navigator.vibrate) {
        switch (type) {
            case 'start':
                navigator.vibrate(100);
                break;
            case 'pause':
                navigator.vibrate([100, 100, 100]);
                break;
            case 'stop':
                navigator.vibrate(200);
                break;
            case 'complete':
                navigator.vibrate([100, 50, 100, 50, 100]);
                break;
            case 'emergency':
                navigator.vibrate([200, 100, 200, 100, 200]);
                break;
            case 'error':
                navigator.vibrate([50, 50, 50, 50, 50]);
                break;
        }
    }
    
    // 这里可以添加实际的音效播放代码
    console.log(`Playing sound: ${type}`);
}

// 退出确认
function confirmExit() {
    if (taskStatus === 'running') {
        showConfirmDialog(
            '退出确认',
            '任务正在执行中，确定要退出吗？',
            '退出后任务将继续在后台执行',
            '⚠️',
            () => {
                window.location.href = '../index.html';
            }
        );
    } else {
        window.location.href = '../index.html';
    }
}

// 消息提示函数
function showMessage(text, type = 'info') {
    if (window.Message) {
        Message.show(text, type);
    } else {
        alert(text);
    }
}

// 菜单功能函数
function switchUser() {
    showConfirmDialog(
        '切换用户',
        '确定要切换到其他用户吗？',
        '切换用户后当前会话将结束，请确保已保存所有工作。',
        '👤',
        () => {
            addOperationLog('切换用户', '成功');
            showMessage('正在切换用户...', 'info');
            // 这里可以添加实际的用户切换逻辑
            setTimeout(() => {
                showMessage('用户切换功能开发中', 'info');
            }, 1000);
        }
    );
}

function showSoftwareSettings() {
    showMessage('软件参数配置功能开发中...', 'info');
    addOperationLog('打开软件参数', '信息');
}

function showSystemSettings() {
    showMessage('系统参数配置功能开发中...', 'info');
    addOperationLog('打开系统参数', '信息');
}

function showPrinterConfig() {
    showMessage('打印机配置功能开发中...', 'info');
    addOperationLog('打开打印机配置', '信息');
}

function showAbout() {
    const aboutContent = `
        <div style="text-align: center; padding: 20px;">
            <h3 style="margin-bottom: 16px;">产线采集关联软件</h3>
            <p style="margin-bottom: 8px;"><strong>版本：</strong>v1.0.0</p>
            <p style="margin-bottom: 8px;"><strong>构建日期：</strong>2024-12-01</p>
            <p style="margin-bottom: 8px;"><strong>开发商：</strong>SQL ScanDuo</p>
            <p style="margin-bottom: 16px;"><strong>描述：</strong>专业的产线数据采集与包装关联管理系统</p>
            <div style="border-top: 1px solid #ddd; padding-top: 16px; margin-top: 16px;">
                <p style="font-size: 12px; color: #666;">
                    Copyright © 2024 SQL ScanDuo. All rights reserved.
                </p>
            </div>
        </div>
    `;
    
    const modal = Modal.show(aboutContent, {
        title: '关于系统',
        width: '400px',
        footer: false,
        buttons: [
            {
                text: '确定',
                class: 'btn-primary'
            }
        ]
    });
    
    addOperationLog('查看系统信息', '信息');
}

// 全局函数
window.startTask = startTask;
window.pauseTask = pauseTask;
window.continueTask = continueTask;
window.stopTask = stopTask;
window.emergencyStop = emergencyStop;
window.refreshStatus = refreshStatus;
window.showDeviceStatus = showDeviceStatus;
window.closeDeviceStatusDialog = closeDeviceStatusDialog;
window.refreshDeviceStatus = refreshDeviceStatus;
window.showOperationLog = showOperationLog;
window.closeOperationLogDialog = closeOperationLogDialog;
window.filterOperationLog = filterOperationLog;
window.exportOperationLog = exportOperationLog;
window.showHelp = showHelp;
window.closeHelpDialog = closeHelpDialog;
window.closeConfirmDialog = closeConfirmDialog;
window.executeConfirmedAction = executeConfirmedAction;
window.showEmergencyDialog = showEmergencyDialog;
window.closeEmergencyDialog = closeEmergencyDialog;
window.startEmergencyCountdown = startEmergencyCountdown;
window.stopEmergencyCountdown = stopEmergencyCountdown;
window.confirmExit = confirmExit; 
window.refreshDataTable = refreshDataTable;
window.exportData = exportData;
window.clearLog = clearLog;

// 新增菜单功能函数
window.switchUser = switchUser;
window.showSoftwareSettings = showSoftwareSettings;
window.showSystemSettings = showSystemSettings;
window.showPrinterConfig = showPrinterConfig;
window.showAbout = showAbout; 

// Tab切换功能 - 已移除，改为单一数据接收区表格
// function switchTab(tabName) {
//     // 更新Tab按钮状态
//     document.querySelectorAll('.tab-btn').forEach(btn => {
//         btn.classList.remove('active');
//     });
//     event.target.classList.add('active');
//     
//     // 更新Tab内容显示
//     document.querySelectorAll('.tab-content').forEach(content => {
//         content.classList.remove('active');
//     });
//     document.getElementById(tabName + '-tab').classList.add('active');
//     
//     currentTab = tabName;
// }

// 更新数据接收区表格 (原单码记录表格)
function updateCollectionTable() {
    const tbody = document.getElementById('collectionTableBody');
    if (!tbody) return;
    
    // 保留现有的示例数据，实际使用时可以替换为动态数据
    // 这里可以根据实际需要添加动态更新逻辑
    console.log('数据接收区表格已更新');
}

// 保持原函数名以兼容现有调用
function updateSingleCodeTable() {
    updateCollectionTable();
}

// 更新包装关联记录表格 - 已移除，功能合并到数据接收区表格
// function updatePackageRelationTable() {
//     const tbody = document.getElementById('packageRelationTableBody');
//     if (!tbody) return;
//     
//     tbody.innerHTML = '';
//     
//     const recentRecords = packageRelationRecords.slice(-50).reverse();
//     
//     recentRecords.forEach((record, index) => {
//         const row = document.createElement('tr');
//         row.innerHTML = `
//             <td>${recentRecords.length - index}</td>
//             <td>${record.parentCode}</td>
//             <td>${record.childCode}</td>
//             <td>${record.relationTime}</td>
//             <td><span class="status-tag ${record.status === '成功' ? 'completed' : 'error'}">${record.status}</span></td>
//         `;
//         tbody.appendChild(row);
//     });
// }

// 模拟单码采集
function simulateSingleCodeCollection() {
    if (taskStatus !== 'running') return;
    
    const levelPositions = ['L1-瓶码', 'L2-箱码', 'L3-垛码'];
    const levelPosition = levelPositions[Math.floor(Math.random() * levelPositions.length)];
    
    const record = {
        levelPosition: levelPosition,
        singleCode: generateSingleCode(levelPosition),
        collectTime: new Date().toLocaleTimeString(),
        result: Math.random() > 0.02 ? '合格' : '不合格'
    };
    
    singleCodeRecords.push(record);
    
    // 更新统计数据
    currentTask.totalInspected++;
    if (record.result === '合格') {
        currentTask.qualifiedCount++;
    } else {
        currentTask.unqualifiedCount++;
        // 添加报警
        addAlarm('质量异常', `检测到不合格产品: ${record.singleCode}`);
    }
    
    // 更新包装统计
    if (levelPosition === 'L2-箱码' && record.result === '合格') {
        currentTask.currentBoxes++;
        if (currentTask.currentBoxes >= currentTask.boxesPerPallet) {
            currentTask.currentPallets++;
            currentTask.currentBoxes = 0;
            currentTask.packageTotal++;
        }
    }
    
    // 更新进度
    currentTask.progress = Math.floor((currentTask.qualifiedCount / currentTask.plannedQty) * 100);
    currentTask.completionRate = Math.floor((currentTask.packageTotal / Math.ceil(currentTask.plannedQty / (currentTask.boxesPerPallet * 100))) * 100);
    
    // 更新界面
    updateTaskInfo();
    updateCollectionTable();
    
    // 模拟包装关联
    if (Math.random() > 0.7) {
        simulatePackageRelation();
    }
}

// 模拟包装关联
function simulatePackageRelation() {
    const record = {
        parentCode: generatePackageCode('BOX'),
        childCode: generatePackageCode('BTL'),
        relationTime: new Date().toLocaleTimeString(),
        status: Math.random() > 0.01 ? '成功' : '失败'
    };
    
    packageRelationRecords.push(record);
    
    // 更新数据接收区表格
    updateCollectionTable();
    
    if (record.status === '失败') {
        addAlarm('关联异常', `包装关联失败: ${record.parentCode} -> ${record.childCode}`);
    }
}

// 生成单码
function generateSingleCode(levelPosition) {
    const prefixes = {
        'L1-瓶码': 'BTL',
        'L2-箱码': 'BOX', 
        'L3-垛码': 'PLT'
    };
    
    const prefix = prefixes[levelPosition] || 'UNK';
    const timestamp = Date.now().toString().slice(-8);
    const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    return `${prefix}${timestamp}${random}`;
}

// 生成包装码
function generatePackageCode(type) {
    const timestamp = Date.now().toString().slice(-8);
    const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    return `${type}${timestamp}${random}`;
}

// 添加报警
function addAlarm(type, content) {
    const alarm = {
        time: new Date().toLocaleTimeString(),
        type: type,
        content: content,
        level: type === '质量异常' ? 'critical' : 'warning'
    };
    
    alarmRecords.push(alarm);
    updateAlarmList();
    
    // 添加到操作日志
    addOperationLog(`报警: ${type}`, '警告', content);
}

// 更新报警列表
function updateAlarmList() {
    const alarmList = document.getElementById('alarmList');
    if (!alarmList) return;
    
    alarmList.innerHTML = '';
    
    const recentAlarms = alarmRecords.slice(-10).reverse();
    
    recentAlarms.forEach(alarm => {
        const alarmItem = document.createElement('div');
        alarmItem.className = `alarm-item ${alarm.level}`;
        alarmItem.innerHTML = `
            <span class="alarm-time">${alarm.time}</span>
            <span class="alarm-content">${alarm.content}</span>
        `;
        alarmList.appendChild(alarmItem);
    });
}

// 完成任务
function completeTask() {
    if (taskStatus !== 'running') return;
    
    taskStatus = 'completed';
    currentTask.status = 'completed';
    currentTask.progress = 100;
    
    stopProductionSimulation();
    stopRunningTimer();
    
    updateTaskInfo();
    updateControlButtons();
    
    addOperationLog('任务完成', '成功');
    showMessage('任务执行完成！', 'success');
    
    playSound('complete');
}

// 辅助功能
function forceFillPallet() {
    if (taskStatus !== 'running') {
        showMessage('请先启动任务', 'warning');
        return;
    }
    
    currentTask.currentBoxes = currentTask.boxesPerPallet;
    currentTask.currentPallets++;
    currentTask.packageTotal++;
    currentTask.currentBoxes = 0;
    
    updateTaskInfo();
    addOperationLog('强制满垛', '成功', `垛数: ${currentTask.currentPallets}`);
    showMessage('强制满垛操作完成', 'success');
}

function resetCounter() {
    currentTask.currentBoxes = 0;
    currentTask.currentPallets = 0;
    currentTask.packageTotal = 0;
    
    updateTaskInfo();
    addOperationLog('重置计数', '成功');
    showMessage('计数器已重置', 'success');
}

function exportReport() {
    showMessage('正在生成报告...', 'info');
    
    setTimeout(() => {
        const reportData = generateReportData();
        downloadReport(reportData);
        showMessage('报告导出完成', 'success');
    }, 2000);
}

function generateReportData() {
    return {
        taskInfo: currentTask,
        singleCodeRecords: singleCodeRecords,
        packageRelationRecords: packageRelationRecords,
        alarmRecords: alarmRecords,
        operationLogs: operationLogs,
        exportTime: new Date().toLocaleString()
    };
}

function downloadReport(data) {
    const jsonContent = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonContent], { type: 'application/json' });
    const link = document.createElement('a');
    
    if (link.download !== undefined) {
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', `生产报告_${currentTask.productionOrder}_${new Date().toISOString().split('T')[0]}.json`);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}

// 重写生产模拟函数
function startProductionSimulation() {
    if (productionTimer) {
        clearInterval(productionTimer);
    }
    
    productionTimer = setInterval(() => {
        if (taskStatus === 'running') {
            // 模拟单码采集（每2秒采集1-2个）
            const collectCount = Math.floor(Math.random() * 2) + 1;
            
            for (let i = 0; i < collectCount; i++) {
                if (currentTask.qualifiedCount < currentTask.plannedQty) {
                    simulateSingleCodeCollection();
                }
            }
            
            // 检查是否完成
            if (currentTask.qualifiedCount >= currentTask.plannedQty) {
                completeTask();
            }
            
            // 模拟偶发系统异常
            if (Math.random() < 0.001) {
                addAlarm('系统异常', '设备通信超时');
            }
        }
    }, 2000);
} 

// 辅助功能
function forceFillPallet() {
    if (taskStatus !== 'running') {
        showMessage('请先启动任务', 'warning');
        return;
    }
    
    currentTask.currentBoxes = currentTask.boxesPerPallet;
    currentTask.currentPallets++;
    currentTask.packageTotal++;
    currentTask.currentBoxes = 0;
    
    updateTaskInfo();
    addOperationLog('强制满垛', '成功', `垛数: ${currentTask.currentPallets}`);
    showMessage('强制满垛操作完成', 'success');
}

// 切换任务状态（启用/停止）
function toggleTask() {
    if (taskStatus === 'pending' || taskStatus === 'stopped') {
        // 显示启用确认对话框
        showConfirmDialog(
            '启用任务',
            '⚠️',
            '确定要启用当前任务吗？',
            '启用后任务将开始执行，请确保设备状态正常。',
            function() {
                startTask();
                updateToggleButton();
            }
        );
    } else if (taskStatus === 'running') {
        // 显示停止确认对话框
        showConfirmDialog(
            '停止任务',
            '⚠️',
            '确定要停止当前任务吗？',
            '停止后可以重新启用任务继续执行。',
            function() {
                stopTask();
                updateToggleButton();
            }
        );
    }
}

// 更新切换按钮状态
function updateToggleButton() {
    const toggleBtn = document.getElementById('toggleBtn');
    const toggleIcon = document.getElementById('toggleIcon');
    const toggleText = document.getElementById('toggleText');
    
    if (taskStatus === 'running') {
        toggleBtn.className = 'control-btn toggle-btn stop-state';
        toggleIcon.textContent = '⏹️';
        toggleText.textContent = '停止';
    } else {
        toggleBtn.className = 'control-btn toggle-btn';
        toggleIcon.textContent = '▶️';
        toggleText.textContent = '启用';
    }
}

// 完成订单
function completeOrder() {
    showConfirmDialog(
        '完成订单',
        '✅',
        '确定要完成当前订单吗？',
        '完成后将结束当前生产任务，请确保所有产品都已正确处理。',
        function() {
            completeTask();
            updateToggleButton();
            updateCompleteOrderButton();
        }
    );
}

// 更新完成订单按钮状态
function updateCompleteOrderButton() {
    const completeOrderBtn = document.getElementById('completeOrderBtn');
    
    if (taskStatus === 'running') {
        completeOrderBtn.disabled = false;
    } else {
        completeOrderBtn.disabled = true;
    }
}

// 添加码功能
function addCode() {
    if (taskStatus !== 'running') {
        showMessage('请先启用任务后再执行此操作', 'warning');
        return;
    }
    
    showConfirmDialog(
        '添加码',
        '➕',
        '确定要添加一个新的产品码吗？',
        '此操作将在当前生产批次中添加一个新的产品码。',
        function() {
            // 模拟添加码操作
            const newCode = generateProductCode();
            addOperationLog('添加码', '成功', `新增产品码: ${newCode}`);
            showMessage(`成功添加产品码: ${newCode}`, 'success');
            
            // 更新统计数据
            currentTask.totalInspected++;
            currentTask.qualifiedCount++;
            updateTaskInfo();
        }
    );
}

// 清除码功能
function clearCode() {
    if (taskStatus !== 'running') {
        showMessage('请先启用任务后再执行此操作', 'warning');
        return;
    }
    
    showConfirmDialog(
        '清除码',
        '🧹',
        '确定要清除当前批次的所有产品码吗？',
        '此操作将清除当前生产批次中的所有产品码，请谨慎操作。',
        function() {
            // 模拟清除码操作
            const clearedCount = Math.floor(Math.random() * 10) + 1;
            addOperationLog('清除码', '成功', `清除了 ${clearedCount} 个产品码`);
            showMessage(`成功清除 ${clearedCount} 个产品码`, 'success');
            
            // 更新统计数据
            currentTask.totalInspected = Math.max(0, currentTask.totalInspected - clearedCount);
            currentTask.qualifiedCount = Math.max(0, currentTask.qualifiedCount - clearedCount);
            updateTaskInfo();
        }
    );
}

// 删除无码功能
function deleteNoCode() {
    if (taskStatus !== 'running') {
        showMessage('请先启用任务后再执行此操作', 'warning');
        return;
    }
    
    showConfirmDialog(
        '删除无码',
        '🗑️',
        '确定要删除所有无码产品吗？',
        '此操作将删除当前批次中所有没有产品码的产品记录。',
        function() {
            // 模拟删除无码操作
            const deletedCount = Math.floor(Math.random() * 5) + 1;
            addOperationLog('删除无码', '成功', `删除了 ${deletedCount} 个无码产品`);
            showMessage(`成功删除 ${deletedCount} 个无码产品`, 'success');
            
            // 更新统计数据
            currentTask.unqualifiedCount = Math.max(0, currentTask.unqualifiedCount - deletedCount);
            updateTaskInfo();
        }
    );
}

// 生成产品码
function generateProductCode() {
    const timestamp = new Date().toISOString().replace(/[-:T.]/g, '').slice(0, 14);
    const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    return `${timestamp}${random}`;
}

// 指定当前箱数功能
function setCurrentBoxCount() {
    if (taskStatus !== 'running') {
        showMessage('请先启用任务后再执行此操作', 'warning');
        return;
    }
    
    // 创建输入对话框
    const currentBoxes = currentTask.currentBoxes || 0;
    const maxBoxes = currentTask.boxesPerPallet || 12;
    
    showConfirmDialog(
        '指定当前箱数',
        '📦',
        '请输入要设置的当前箱数：',
        `<div style="margin: 16px 0;">
            <label style="display: block; margin-bottom: 8px; font-weight: bold;">当前箱数：</label>
            <input type="number" id="boxCountInput" value="${currentBoxes}" min="0" max="${maxBoxes}" 
                   style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px;">
            <div style="margin-top: 8px; font-size: 12px; color: #666;">
                范围：0 - ${maxBoxes} 箱（每垛最大箱数：${maxBoxes}）
            </div>
        </div>`,
        function() {
            const inputValue = document.getElementById('boxCountInput').value;
            const newBoxCount = parseInt(inputValue);
            
            if (isNaN(newBoxCount) || newBoxCount < 0 || newBoxCount > maxBoxes) {
                showMessage(`请输入有效的箱数（0-${maxBoxes}）`, 'error');
                return;
            }
            
            currentTask.currentBoxes = newBoxCount;
            updateTaskInfo();
            addOperationLog('指定当前箱数', '成功', `设置当前箱数为: ${newBoxCount}`);
            showMessage(`当前箱数已设置为: ${newBoxCount}`, 'success');
        }
    );
}

// 读码剔除功能
function removeCodeReading() {
    if (taskStatus !== 'running') {
        showMessage('请先启用任务后再执行此操作', 'warning');
        return;
    }
    
    // 切换剔除功能状态
    removeFeatureEnabled = !removeFeatureEnabled;
    updateRemoveStatus();
    
    if (removeFeatureEnabled) {
        showConfirmDialog(
            '开启读码剔除',
            '🔍',
            '确定要开启自动读码剔除功能吗？',
            `<div style="margin: 16px 0;">
                <p style="margin-bottom: 12px;">开启后系统将自动：</p>
                <ul style="margin: 0; padding-left: 20px; line-height: 1.6;">
                    <li>实时检测并剔除无效的产品码</li>
                    <li>自动移除重复的产品码记录</li>
                    <li>持续清理异常的读码数据</li>
                </ul>
                <div style="margin-top: 12px; padding: 8px; background: #e3f2fd; border: 1px solid #2196f3; border-radius: 4px; font-size: 12px;">
                    <strong>提示：</strong>功能开启后将在状态栏显示"已开启"状态。
                </div>
            </div>`,
            function() {
                addOperationLog('开启读码剔除', '成功', '自动剔除功能已启用');
                showMessage('读码剔除功能已开启', 'success');
                
                // 模拟添加一些日志记录到接收数据区
                setTimeout(() => {
                    addCollectionLog('读码剔除功能', '系统启用', '已开启');
                }, 500);
            }
        );
    } else {
        addOperationLog('关闭读码剔除', '成功', '自动剔除功能已关闭');
        showMessage('读码剔除功能已关闭', 'info');
        
        // 模拟添加一些日志记录到接收数据区
        setTimeout(() => {
            addCollectionLog('读码剔除功能', '系统关闭', '已关闭');
        }, 500);
    }
}

// 添加数据接收区日志记录的辅助函数
function addCollectionLog(content, code, status) {
    const collectionLogList = document.getElementById('collectionLogList');
    if (!collectionLogList) return;
    
    const logEntry = document.createElement('div');
    logEntry.className = 'log-entry';
    
    const now = new Date();
    const timeString = now.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        fractionalSecondDigits: 3
    });
    
    const statusClass = status === '完成' ? 'success' : 
                       status === '失败' ? 'failed' : 
                       status === '警告' ? 'duplicate' : 'success';
    
    logEntry.innerHTML = `
        <span class="log-time">${timeString}</span>
        <span class="log-content">${content}&&&${code}</span>
        <span class="log-status ${statusClass}">${status}</span>
    `;
    
    // 插入到列表顶部
    collectionLogList.insertBefore(logEntry, collectionLogList.firstChild);
    
    // 保持最多100条记录
    while (collectionLogList.children.length > 100) {
        collectionLogList.removeChild(collectionLogList.lastChild);
    }
}

// 新增全局函数导出
// window.switchTab = switchTab; // 已移除tab功能
window.completeTask = completeTask;
window.forceFillPallet = forceFillPallet;
window.toggleTask = toggleTask;
window.completeOrder = completeOrder;
window.addCode = addCode;
window.clearCode = clearCode;
window.deleteNoCode = deleteNoCode;
window.setCurrentBoxCount = setCurrentBoxCount;
window.removeCodeReading = removeCodeReading; 