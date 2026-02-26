// 系统参数配置页面JavaScript功能

// 全局变量
let currentTab = 'io-control';
let currentDeviceLevel = 'level1';
let isConfigChanged = false;

// 模拟数据
let mockIODevices = [];
let mockPrinters = [];
let mockDeviceConfigs = {};
let mockSystemConfig = {};

// 设备管理相关变量
let currentEditingIODeviceId = null;
let currentEditingPrinterId = null;

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('系统参数配置页面初始化...');
    initializePage();
});

function initializePage() {
    // 初始化事件监听
    initEventListeners();
    
    // 生成模拟数据
    generateMockData();
    
    // 初始化界面
    initTabs();
    initDeviceStatus();
    
    // 更新时间显示
    updateTime();
    setInterval(updateTime, 1000);
    
    console.log('系统参数配置页面初始化完成');
}

function initEventListeners() {
    // 选项卡切换
    document.querySelectorAll('.tab-item').forEach(tab => {
        tab.addEventListener('click', function() {
            switchTab(this.dataset.tab);
        });
    });
    
    // 设备选项卡切换
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('device-tab')) {
            switchDeviceTab(e.target.dataset.device);
        }
    });
    
    // IO连接方式切换
    document.addEventListener('change', function(e) {
        if (e.target.name === 'ioConnectionType') {
            toggleIOConnectionConfig(e.target.value);
        }
    });
    
    // 配置变更监听
    document.addEventListener('input', function(e) {
        if (e.target.classList.contains('form-control') || 
            e.target.type === 'checkbox' || 
            e.target.type === 'radio') {
            markConfigChanged();
        }
    });
    
    // ESC键关闭对话框
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeConfirmDialog();
            closeIODeviceEditDialog();
            closePrinterEditDialog();
        }
    });
}

// 生成模拟数据
function generateMockData() {
    // 生成IO设备数据
    mockIODevices = [
        {
            id: 'io001',
            name: 'IO控制盒1',
            type: 'modbus',
            connectionType: 'network',
                    networkConfig: {
                        ipAddress: '192.168.1.101',
                port: 502
                    },
                    serialConfig: {
                        port: 'COM1',
                        baudRate: 9600
            },
            timeout: 5,
            retryCount: 3,
                    enabled: true,
            status: 'online',
            description: '主要IO控制设备',
            lastCommunication: '2024-12-04 15:30:25',
            errorCount: 0,
            responseTime: 45
        },
        {
            id: 'io002',
            name: 'IO控制盒2',
            type: 'plc',
                    connectionType: 'network',
                    networkConfig: {
                        ipAddress: '192.168.1.102',
                port: 502
                    },
                    serialConfig: {
                        port: 'COM2',
                        baudRate: 9600
            },
            timeout: 5,
            retryCount: 3,
                    enabled: true,
            status: 'warning',
            description: '备用IO控制设备',
            lastCommunication: '2024-12-04 15:29:15',
            errorCount: 2,
            responseTime: 120
        },
        {
            id: 'io003',
            name: 'IO模块3',
            type: 'io_module',
            connectionType: 'serial',
                    networkConfig: {
                        ipAddress: '192.168.1.103',
                port: 502
                    },
                    serialConfig: {
                        port: 'COM3',
                baudRate: 19200
            },
            timeout: 3,
            retryCount: 2,
            enabled: false,
            status: 'offline',
            description: '扩展IO模块',
            lastCommunication: '从未连接',
            errorCount: 0,
            responseTime: 0
        }
    ];
    
    // 生成打印机数据
    mockPrinters = [
        {
            id: 'printer001',
            name: '标签打印机1',
            path: 'C:\\PrintModules\\LabelPrinter.exe',
            enabled: true,
            status: 'online',
            description: '用于打印产品标签',
            lastPrint: '2024-12-04 15:20:30',
            printCount: 1250
        },
        {
            id: 'printer002',
            name: '小票打印机',
            path: 'C:\\PrintModules\\ReceiptPrinter.exe',
            enabled: true,
            status: 'offline',
            description: '用于打印小票和报表',
            lastPrint: '2024-12-04 14:45:15',
            printCount: 856
        }
    ];
    
    // 生成系统配置数据
    mockSystemConfig = {
        communication: {
            serial: {
                port: 'COM1',
                baudRate: 9600,
                dataBits: 8,
                stopBits: 1,
                parity: 'none',
                flowControl: 'none'
            },
            network: {
                ipAddress: '192.168.1.100',
                port: 9100,
                protocol: 'tcp',
                timeout: 5,
                retryCount: 3,
                heartbeatInterval: 10
            }
        },
        io: {
            connectionType: 'network',
            ipAddress: '192.168.1.101',
            port: 502,
            serialPort: 'COM2',
            enabled: true
        },
        alarm: {
            methods: ['sound', 'popup'],
            delay: 5,
            interval: 60
        },
        advanced: {
            communicationTimeout: 30,
            retryCount: 3,
            heartbeatInterval: 10,
            logLevel: 'info',
            dataUploadInterval: 300,
            backupInterval: 3600,
            accessControl: false,
            operationLog: true,
            configBackup: true
        }
    };
    
    console.log('生成模拟数据完成');
}

// 初始化选项卡
function initTabs() {
    switchTab('io-control');
}

// 切换选项卡
function switchTab(tabId) {
    // 移除所有活动状态
    document.querySelectorAll('.tab-item').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    
    // 添加活动状态
    document.querySelector(`[data-tab="${tabId}"]`).classList.add('active');
    document.getElementById(tabId).classList.add('active');
    
    currentTab = tabId;
    
    // 根据选项卡初始化内容
    switch(tabId) {
        case 'io-control':
            loadIOControlConfig();
            break;
        case 'print-module':
            loadPrintModuleConfig();
            break;
        case 'alarm-settings':
            loadAlarmSettings();
            break;
    }
}

function createStatusTag(type, text) {
    return `<span class="status-tag ${type}">${text}</span>`;
}

// 加载IO设备列表
function loadIODeviceList() {
    const container = document.getElementById('ioDeviceList');
    if (!container) return;
    
    container.innerHTML = '';
    
    if (mockIODevices.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">🔌</div>
                <div class="empty-text">暂无IO设备</div>
                <button class="btn btn-primary" onclick="addIODevice()">添加第一个IO设备</button>
            </div>
        `;
        return;
    }
    
    mockIODevices.forEach((device, index) => {
        const item = document.createElement('div');
        item.className = 'io-device-item';
        item.innerHTML = `
            <div class="io-device-row">
                <div class="io-device-name-status">
                    <span class="io-device-name">${device.name}</span>
                    <span class="device-status-badge ${device.status}">${getStatusText(device.status)}</span>
                </div>
                <div class="io-device-type">${getIODeviceTypeText(device.type)}</div>
                <div class="io-device-connection">${getConnectionTypeText(device.connectionType)}</div>
                <div class="io-device-address">${getIODeviceAddress(device)}</div>
                <div class="io-device-actions">
                    <button class="btn action-btn" onclick="testIODevice('${device.id}')">测试</button>
                    <button class="btn action-btn" onclick="editIODevice('${device.id}')">编辑</button>
                    <button class="btn btn-danger action-btn" onclick="deleteIODevice('${device.id}')">删除</button>
                </div>
            </div>
        `;
        container.appendChild(item);
    });
}

// 获取IO设备类型文本
function getIODeviceTypeText(type) {
    const typeMap = {
        'modbus': 'Modbus设备',
        'plc': 'PLC设备',
        'io_module': 'IO模块',
        'custom': '自定义设备'
    };
    return typeMap[type] || '未知类型';
}

// 获取IO设备地址
function getIODeviceAddress(device) {
    if (device.connectionType === 'network') {
        return `${device.networkConfig.ipAddress}:${device.networkConfig.port}`;
    } else if (device.connectionType === 'serial') {
        return `${device.serialConfig.port}@${device.serialConfig.baudRate}`;
    }
    return '未配置';
}

// 加载IO控制配置
function loadIOControlConfig() {
    // 加载IO设备列表
    loadIODeviceList();
    
    // 加载剔除装置列表
    loadRejectDeviceList();
}

// IO设备管理函数
function addIODevice() {
    currentEditingIODeviceId = null;
    showIODeviceEditDialog('add');
}

function editIODevice(deviceId) {
    currentEditingIODeviceId = deviceId;
    showIODeviceEditDialog('edit');
}

function copyIODevice(deviceId) {
    const device = mockIODevices.find(d => d.id === deviceId);
    if (!device) return;
    
    const newDevice = {
        ...device,
        id: 'io' + Date.now(),
        name: device.name + ' - 副本',
        enabled: false,
        status: 'offline',
        lastCommunication: '从未连接',
        errorCount: 0,
        responseTime: 0
    };
    
    mockIODevices.push(newDevice);
    loadIODeviceList();
    updateDeviceStatusList(); // 同步更新右侧状态监控
    showMessage('IO设备复制成功', 'success');
}

function deleteIODevice(deviceId) {
    const device = mockIODevices.find(d => d.id === deviceId);
    if (!device) return;
    
    showConfirmDialog(
        '确认删除',
        `确定要删除IO设备 "${device.name}" 吗？`,
        () => {
            mockIODevices = mockIODevices.filter(d => d.id !== deviceId);
            loadIODeviceList();
            updateDeviceStatusList(); // 同步更新右侧状态监控
            showMessage('IO设备删除成功', 'success');
        }
    );
}

function testIODevice(deviceId) {
    const device = mockIODevices.find(d => d.id === deviceId);
    if (!device) return;
    
    showMessage(`正在测试IO设备 "${device.name}"...`, 'info');
    
    setTimeout(() => {
        device.status = 'online';
        device.lastCommunication = new Date().toLocaleString('zh-CN');
        device.responseTime = Math.floor(Math.random() * 100) + 20;
        device.errorCount = 0;
        loadIODeviceList();
        updateDeviceStatusList(); // 同步更新右侧状态监控
        showMessage(`IO设备 "${device.name}" 测试成功`, 'success');
    }, 1500);
}

function testAllIODevices() {
    const enabledDevices = mockIODevices.filter(d => d.enabled);
    if (enabledDevices.length === 0) {
        showMessage('没有启用的IO设备可以测试', 'warning');
        return;
    }
    
    showMessage(`正在测试 ${enabledDevices.length} 个IO设备...`, 'info');
    
    setTimeout(() => {
        enabledDevices.forEach(device => {
            device.status = Math.random() > 0.2 ? 'online' : 'warning';
            device.lastCommunication = new Date().toLocaleString('zh-CN');
            device.responseTime = Math.floor(Math.random() * 150) + 20;
            device.errorCount = device.status === 'warning' ? Math.floor(Math.random() * 5) : 0;
        });
        loadIODeviceList();
        updateDeviceStatusList(); // 同步更新右侧状态监控
        showMessage('所有IO设备测试完成', 'success');
    }, 2000);
}

// 加载剔除装置列表
function loadRejectDeviceList() {
    const container = document.getElementById('rejectDeviceList');
    
    const mockRejectDevices = [
        { level: 1, outputPort: 1, delayMs: 100, pulseWidth: 50, mode: 'single', enabled: true },
        { level: 2, outputPort: 2, delayMs: 150, pulseWidth: 75, mode: 'single', enabled: true },
        { level: 3, outputPort: 3, delayMs: 200, pulseWidth: 100, mode: 'single', enabled: false }
    ];
    
    container.innerHTML = '';
    
    mockRejectDevices.forEach((device, index) => {
        const item = document.createElement('div');
        item.className = 'reject-device-item';
        item.innerHTML = `
            <div class="reject-device-info">
                <div>第${device.level}层</div>
                <div>端口${device.outputPort}</div>
                <div>延时${device.delayMs}ms</div>
                <div>脉冲${device.pulseWidth}ms</div>
            </div>
            <div class="reject-device-actions">
                <button class="btn action-btn" onclick="editRejectDevice(${index})">编辑</button>
                <button class="btn btn-danger action-btn" onclick="deleteRejectDevice(${index})">删除</button>
            </div>
        `;
        container.appendChild(item);
    });
}

// 加载打印模块配置
function loadPrintModuleConfig() {
    loadPrinterList();
}

// 加载打印机列表
function loadPrinterList() {
    const container = document.getElementById('printerList');
    if (!container) return;
    
    container.innerHTML = '';
    
        if (mockPrinters.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">🖨️</div>
                <div class="empty-text">暂无打印机</div>
                <button class="btn btn-primary" onclick="addPrinter()">添加第一台打印机</button>
            </div>
        `;
        return;
    }
    
    mockPrinters.forEach(printer => {
        const item = document.createElement('div');
        item.className = 'printer-item';
        item.innerHTML = `
            <div class="printer-row">
                <div class="printer-name-status">
                    <span class="printer-name">${printer.name}</span>
                    <span class="device-status-badge ${printer.status}">${getStatusText(printer.status)}</span>
                </div>
                <div class="printer-path">${printer.path}</div>
                <div class="printer-actions">
                    <button class="btn action-btn" onclick="testPrinter('${printer.id}')">测试</button>
                    <button class="btn action-btn" onclick="editPrinter('${printer.id}')">编辑</button>
                    <button class="btn btn-danger action-btn" onclick="deletePrinter('${printer.id}')">删除</button>
                </div>
            </div>
        `;
        container.appendChild(item);
    });
}





// 加载报警设置
function loadAlarmSettings() {
    const config = mockSystemConfig.alarm;
    
    // 设置声音报警
    document.getElementById('alarmSound').checked = config.methods.includes('sound');
    
    // 设置参数
    document.getElementById('alarmDelay').value = config.delay;
    document.getElementById('alarmInterval').value = config.interval;
}



// 加载高级设置


// 初始化设备状态监控
function initDeviceStatus() {
    updateDeviceStatusList();
    
    // 定时更新设备状态
    setInterval(updateDeviceStatusList, 5000);
}

// 更新设备状态列表
function updateDeviceStatusList() {
    const container = document.getElementById('deviceStatusList');
    if (!container) return;
    
    container.innerHTML = '';
    
    // 如果没有IO设备，显示空状态
    if (mockIODevices.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📊</div>
                <div class="empty-text">暂无IO设备状态</div>
                <div class="empty-hint">请先在左侧添加IO设备</div>
            </div>
        `;
        return;
    }
    
    // 显示所有IO设备的状态
    mockIODevices.forEach(device => {
        const item = document.createElement('div');
        item.className = 'device-status-item';
        
        // 获取设备地址
        const address = getIODeviceAddress(device);
        
        item.innerHTML = `
            <div class="device-status-header">
                <span class="device-status-name">${device.name}</span>
                <span class="device-status-badge ${device.status}">${getStatusText(device.status)}</span>
            </div>
            <div class="device-status-details">
                <div class="status-detail-item">
                    <span>设备类型:</span>
                    <span>${getIODeviceTypeText(device.type)}</span>
                </div>
                <div class="status-detail-item">
                    <span>连接方式:</span>
                    <span>${getConnectionTypeText(device.connectionType)}</span>
                </div>
                <div class="status-detail-item">
                    <span>地址:</span>
                    <span>${address}</span>
                </div>
                <div class="status-detail-item">
                    <span>响应时间:</span>
                    <span>${device.responseTime}ms</span>
                </div>
                <div class="status-detail-item">
                    <span>错误次数:</span>
                    <span>${device.errorCount}</span>
                </div>
                <div class="status-detail-item">
                    <span>状态:</span>
                    <span>${device.enabled ? '启用' : '禁用'}</span>
                </div>
            </div>
        `;
        
        container.appendChild(item);
    });
}

function getStatusText(status) {
    const statusMap = {
        'online': '在线',
        'offline': '离线',
        'warning': '异常'
    };
    return statusMap[status] || '未知';
}

function getConnectionTypeText(type) {
    const typeMap = {
        'network': '网口',
        'serial': '串口',
        'disabled': '禁用'
    };
    return typeMap[type] || '未知';
}

// 标记配置已更改
function markConfigChanged() {
    isConfigChanged = true;
    updateConfigStatus('配置状态：未保存');
}

// 更新配置状态
function updateConfigStatus(status) {
    const statusElement = document.getElementById('configStatus');
    if (statusElement) {
        statusElement.textContent = status;
    }
}

// 更新时间显示
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

// 显示消息
function showMessage(text, type = 'info') {
    console.log(`[${type}] ${text}`);
    
    // 创建消息元素
    const message = document.createElement('div');
    message.className = `message ${type}`;
    message.style.cssText = `
        position: fixed;
        top: 50px;
        right: 20px;
        padding: 12px 20px;
        border-radius: 4px;
        color: white;
        font-size: 12px;
        z-index: 10000;
        max-width: 300px;
        word-wrap: break-word;
    `;
    
    // 根据类型设置颜色
    switch(type) {
        case 'success':
            message.style.backgroundColor = '#4caf50';
            break;
        case 'warning':
            message.style.backgroundColor = '#ff9800';
            break;
        case 'error':
            message.style.backgroundColor = '#f44336';
            break;
        default:
            message.style.backgroundColor = '#2196f3';
    }
    
    message.textContent = text;
    
    // 添加到页面
    document.body.appendChild(message);
    
    // 3秒后自动移除
    setTimeout(() => {
        if (message.parentNode) {
            message.parentNode.removeChild(message);
        }
    }, 3000);
}

// 功能函数
function applyIOConfig() {
    showMessage('IO控制配置应用成功', 'success');
    markConfigChanged();
}

function testRejectDevice() {
    showMessage('正在测试剔除装置...', 'info');
    
    setTimeout(() => {
        showMessage('剔除装置测试成功', 'success');
    }, 1000);
}

function addRejectDevice() {
    showMessage('添加剔除装置功能待实现', 'info');
}

function editRejectDevice(index) {
    showMessage('编辑剔除装置功能待实现', 'info');
}

function deleteRejectDevice(index) {
    showMessage('删除剔除装置功能待实现', 'info');
}

function testPrint() {
    showMessage('正在执行测试打印...', 'info');
    
    setTimeout(() => {
        showMessage('测试打印成功', 'success');
    }, 2000);
}

function applyPrintConfig() {
    showMessage('打印模块配置应用成功', 'success');
    markConfigChanged();
}

// 打印机管理函数
function addPrinter() {
    currentEditingPrinterId = null;
    showPrinterEditDialog('add');
}

function editPrinter(printerId) {
    currentEditingPrinterId = printerId;
    showPrinterEditDialog('edit');
}

function deletePrinter(printerId) {
    const printer = mockPrinters.find(p => p.id === printerId);
    if (!printer) return;
    
    showConfirmDialog(
        '确认删除',
        `确定要删除打印机 "${printer.name}" 吗？`,
        () => {
            mockPrinters = mockPrinters.filter(p => p.id !== printerId);
            loadPrinterList();
            showMessage('打印机删除成功', 'success');
        }
    );
}

function testPrinter(printerId) {
    const printer = mockPrinters.find(p => p.id === printerId);
    if (!printer) return;
    
    showMessage(`正在测试打印机 "${printer.name}"...`, 'info');
    
    setTimeout(() => {
        printer.status = 'online';
        printer.lastPrint = new Date().toLocaleString('zh-CN');
        printer.printCount += 1;
        loadPrinterList();
        showMessage(`打印机 "${printer.name}" 测试成功`, 'success');
    }, 1500);
}



function testAlarm() {
    showMessage('正在测试报警功能...', 'info');
    
    setTimeout(() => {
        showMessage('报警测试成功', 'success');
    }, 1000);
}

function applyAlarmConfig() {
    showMessage('报警设置应用成功', 'success');
    markConfigChanged();
}

function resetAlarmConfig() {
    showConfirmDialog(
        '确认重置',
        '确定要重置报警配置吗？',
        () => {
            loadAlarmSettings();
            showMessage('报警配置已重置', 'info');
        }
    );
}



// 对话框函数
function showConfirmDialog(title, message, callback) {
    const dialog = document.getElementById('confirmDialog');
    const titleElement = document.getElementById('confirmTitle');
    const messageElement = document.getElementById('confirmMessage');
    const confirmBtn = document.getElementById('confirmBtn');
    
    titleElement.textContent = title;
    messageElement.textContent = message;
    
    confirmBtn.onclick = () => {
        closeConfirmDialog();
        if (callback) callback();
    };
    
    dialog.style.display = 'flex';
}

function closeConfirmDialog() {
    const dialog = document.getElementById('confirmDialog');
    dialog.style.display = 'none';
}

// IO设备编辑对话框函数
function showIODeviceEditDialog(mode) {
    const dialog = document.getElementById('ioDeviceEditDialog');
    const title = document.getElementById('ioDeviceEditTitle');
    
    if (mode === 'add') {
        title.textContent = '添加IO设备';
        clearIODeviceEditForm();
    } else {
        title.textContent = '编辑IO设备';
        loadIODeviceEditForm();
    }
    
    dialog.style.display = 'flex';
}

function closeIODeviceEditDialog() {
    const dialog = document.getElementById('ioDeviceEditDialog');
    dialog.style.display = 'none';
    currentEditingIODeviceId = null;
}

function clearIODeviceEditForm() {
    document.getElementById('editIODeviceName').value = '';
    document.getElementById('editIODeviceType').value = 'modbus';
    document.querySelector('input[name="editIOConnectionType"][value="network"]').checked = true;
    document.getElementById('editIOIpAddress').value = '';
    document.getElementById('editIOPort').value = '';
    document.getElementById('editIOSerialPort').value = '';
    document.getElementById('editIOBaudRate').value = '9600';
    document.getElementById('editIOTimeout').value = '5';
    document.getElementById('editIORetryCount').value = '3';
    document.getElementById('editIODeviceEnabled').checked = true;
    document.getElementById('editIODeviceDescription').value = '';
    
    toggleEditIOConnectionConfig('network');
}

function loadIODeviceEditForm() {
    const device = mockIODevices.find(d => d.id === currentEditingIODeviceId);
    if (!device) return;
    
    document.getElementById('editIODeviceName').value = device.name;
    document.getElementById('editIODeviceType').value = device.type;
    document.querySelector(`input[name="editIOConnectionType"][value="${device.connectionType}"]`).checked = true;
    
    if (device.connectionType === 'network') {
        document.getElementById('editIOIpAddress').value = device.networkConfig.ipAddress;
        document.getElementById('editIOPort').value = device.networkConfig.port;
    } else if (device.connectionType === 'serial') {
        document.getElementById('editIOSerialPort').value = device.serialConfig.port;
        document.getElementById('editIOBaudRate').value = device.serialConfig.baudRate;
    }
    
    document.getElementById('editIOTimeout').value = device.timeout;
    document.getElementById('editIORetryCount').value = device.retryCount;
    
    if (device.enabled) {
        document.getElementById('editIODeviceEnabled').checked = true;
    } else {
        document.getElementById('editIODeviceDisabled').checked = true;
    }
    
    document.getElementById('editIODeviceDescription').value = device.description || '';
    
    toggleEditIOConnectionConfig(device.connectionType);
}

function toggleEditIOConnectionConfig(type) {
    const networkConfig = document.getElementById('editIONetworkConfig');
    const serialConfig = document.getElementById('editIOSerialConfig');
    
    if (type === 'network') {
        networkConfig.style.display = 'flex';
        serialConfig.style.display = 'none';
    } else if (type === 'serial') {
        networkConfig.style.display = 'none';
        serialConfig.style.display = 'flex';
    }
}

function saveIODeviceEdit() {
    const name = document.getElementById('editIODeviceName').value.trim();
    const type = document.getElementById('editIODeviceType').value;
    const connectionType = document.querySelector('input[name="editIOConnectionType"]:checked').value;
    
    if (!name) {
        showMessage('请填写设备名称', 'warning');
        return;
    }
    
    let networkConfig = {};
    let serialConfig = {};
    
    if (connectionType === 'network') {
        const ipAddress = document.getElementById('editIOIpAddress').value.trim();
        const port = document.getElementById('editIOPort').value;
        
        if (!ipAddress || !port) {
            showMessage('请填写IP地址和端口号', 'warning');
            return;
        }
        
        networkConfig = { ipAddress, port: parseInt(port) };
    } else if (connectionType === 'serial') {
        const serialPort = document.getElementById('editIOSerialPort').value;
        const baudRate = document.getElementById('editIOBaudRate').value;
        
        if (!serialPort) {
            showMessage('请选择串口号', 'warning');
        return;
    }
    
        serialConfig = { port: serialPort, baudRate: parseInt(baudRate) };
    }
    
    const deviceData = {
        name: name,
        type: type,
        connectionType: connectionType,
        networkConfig: networkConfig,
        serialConfig: serialConfig,
        timeout: parseInt(document.getElementById('editIOTimeout').value) || 5,
        retryCount: parseInt(document.getElementById('editIORetryCount').value) || 3,
        enabled: document.getElementById('editIODeviceEnabled').checked,
        description: document.getElementById('editIODeviceDescription').value.trim()
    };
    
    if (currentEditingIODeviceId) {
        // 编辑设备
        const device = mockIODevices.find(d => d.id === currentEditingIODeviceId);
        if (device) {
            Object.assign(device, deviceData);
            showMessage('IO设备更新成功', 'success');
        }
    } else {
        // 新增设备
        const newDevice = {
            ...deviceData,
            id: 'io' + Date.now(),
            status: 'offline',
            lastCommunication: '从未连接',
            errorCount: 0,
            responseTime: 0
        };
        mockIODevices.push(newDevice);
        showMessage('IO设备创建成功', 'success');
    }
    
    closeIODeviceEditDialog();
    loadIODeviceList();
    updateDeviceStatusList(); // 同步更新右侧状态监控
    markConfigChanged();
}

// 打印机编辑对话框函数
function showPrinterEditDialog(mode) {
    const dialog = document.getElementById('printerEditDialog');
    const title = document.getElementById('printerEditTitle');
    
    if (mode === 'add') {
        title.textContent = '添加打印机';
        clearPrinterEditForm();
    } else {
        title.textContent = '编辑打印机';
        loadPrinterEditForm();
    }
    
    dialog.style.display = 'flex';
}

function closePrinterEditDialog() {
    const dialog = document.getElementById('printerEditDialog');
    dialog.style.display = 'none';
    currentEditingPrinterId = null;
}

function clearPrinterEditForm() {
    document.getElementById('editPrinterName').value = '';
    document.getElementById('editPrinterPath').value = '';
    document.getElementById('editPrinterEnabled').checked = true;
    document.getElementById('editPrinterDescription').value = '';
}

function loadPrinterEditForm() {
    const printer = mockPrinters.find(p => p.id === currentEditingPrinterId);
    if (!printer) return;
    
    document.getElementById('editPrinterName').value = printer.name;
    document.getElementById('editPrinterPath').value = printer.path;
    document.getElementById('editPrinterDescription').value = printer.description || '';
    
    if (printer.enabled) {
        document.getElementById('editPrinterEnabled').checked = true;
    } else {
        document.getElementById('editPrinterDisabled').checked = true;
    }
}

function selectPrinterPath() {
    // 模拟文件选择对话框
    showMessage('请选择打印模块路径（实际应用中会打开文件选择对话框）', 'info');
    // 这里应该调用系统的文件选择对话框
    // 示例路径
    const examplePath = 'C:\\PrintModules\\Printer_' + Date.now() + '.exe';
    document.getElementById('editPrinterPath').value = examplePath;
}

function savePrinterEdit() {
    const name = document.getElementById('editPrinterName').value.trim();
    const path = document.getElementById('editPrinterPath').value.trim();
    
    if (!name) {
        showMessage('请填写打印机名称', 'warning');
        return;
    }
    
    if (!path) {
        showMessage('请选择打印模块路径', 'warning');
        return;
    }
    
    const printerData = {
        name: name,
        path: path,
        enabled: document.getElementById('editPrinterEnabled').checked,
        description: document.getElementById('editPrinterDescription').value.trim()
    };
    
    if (currentEditingPrinterId) {
        // 编辑打印机
        const printer = mockPrinters.find(p => p.id === currentEditingPrinterId);
        if (printer) {
            Object.assign(printer, printerData);
            showMessage('打印机更新成功', 'success');
        }
    } else {
        // 新增打印机
        const newPrinter = {
            ...printerData,
            id: 'printer' + Date.now(),
            status: 'offline',
            lastPrint: '从未打印',
            printCount: 0
        };
        mockPrinters.push(newPrinter);
        showMessage('打印机创建成功', 'success');
    }
    
    closePrinterEditDialog();
    loadPrinterList();
    markConfigChanged();
}



// 占位函数
function confirmAction() {
    // 由具体的确认操作设置
}



// 全局函数导出
window.addIODevice = addIODevice;
window.editIODevice = editIODevice;
window.copyIODevice = copyIODevice;
window.deleteIODevice = deleteIODevice;
window.testIODevice = testIODevice;
window.testAllIODevices = testAllIODevices;
window.showIODeviceEditDialog = showIODeviceEditDialog;
window.closeIODeviceEditDialog = closeIODeviceEditDialog;
window.toggleEditIOConnectionConfig = toggleEditIOConnectionConfig;
window.saveIODeviceEdit = saveIODeviceEdit;
window.testRejectDevice = testRejectDevice;
window.applyIOConfig = applyIOConfig;
window.addRejectDevice = addRejectDevice;
window.editRejectDevice = editRejectDevice;
window.deleteRejectDevice = deleteRejectDevice;
window.testPrint = testPrint;
window.applyPrintConfig = applyPrintConfig;
window.addPrinter = addPrinter;
window.editPrinter = editPrinter;
window.deletePrinter = deletePrinter;
window.testPrinter = testPrinter;
window.showPrinterEditDialog = showPrinterEditDialog;
window.closePrinterEditDialog = closePrinterEditDialog;
window.selectPrinterPath = selectPrinterPath;
window.savePrinterEdit = savePrinterEdit;
window.testAlarm = testAlarm;
window.applyAlarmConfig = applyAlarmConfig;
window.resetAlarmConfig = resetAlarmConfig;
window.closeConfirmDialog = closeConfirmDialog;
window.confirmAction = confirmAction;

console.log('系统参数配置脚本加载完成'); 