// 数据上传管理页面JavaScript功能

// 全局变量
let currentOrderPage = 1;
let orderPageSize = 20;
let totalOrders = 0;
let filteredOrders = [];
let selectedOrder = null;

let currentDataPage = 1;
let dataPageSize = 50;
let totalTaskData = 0;
let filteredTaskData = [];

let uploadProgress = {
    isUploading: false,
    currentOrder: null,
    progress: 0,
    status: 'ready'
};

// 模拟生产订单数据
let mockOrders = [];

// 模拟生产任务数据
let mockTaskData = [];

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('数据上传管理页面初始化...');
    initializePage();
});

function initializePage() {
    // 初始化事件监听
    initEventListeners();
    
    // 生成模拟数据
    generateMockData();
    
    // 加载订单列表
    loadOrderList();
    
    // 更新时间显示
    updateTime();
    setInterval(updateTime, 1000);
    
    console.log('数据上传管理页面初始化完成');
}

function initEventListeners() {
    // 查询码输入框
    const codeSearch = document.getElementById('codeSearch');
    if (codeSearch) {
        codeSearch.addEventListener('input', debounce(handleCodeSearch, 300));
        codeSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchCode();
            }
        });
    }
    
    // 分页大小选择
    const dataPageSizeSelect = document.getElementById('dataPageSizeSelect');
    if (dataPageSizeSelect) {
        dataPageSizeSelect.addEventListener('change', handleDataPageSizeChange);
    }
    
    // ESC键关闭对话框
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeCodeReplaceModal();
            closeCodeDeleteModal();
            closeUploadProgress();
        }
    });
}

// 生成模拟数据
function generateMockData() {
    // 生成生产订单数据
    mockOrders = [
        {
            id: 1,
            orderNo: 'PO202412010001',
            productName: '维生素C片',
            plannedQty: 5000,
            completedQty: 4980,
            productionDate: '2024-12-01',
            productionStatus: '生产完成',
            uploadStatus: '上传完成',
            batchNumber: 'B2024001',
            createTime: '2024-12-01 08:00:00'
        },
        {
            id: 2,
            orderNo: 'PO202412010002',
            productName: '感冒灵颗粒',
            plannedQty: 3000,
            completedQty: 2995,
            productionDate: '2024-12-01',
            productionStatus: '生产完成',
            uploadStatus: '上传中',
            batchNumber: 'B2024002',
            createTime: '2024-12-01 09:00:00'
        },
        {
            id: 3,
            orderNo: 'PO202412010003',
            productName: '阿莫西林胶囊',
            plannedQty: 8000,
            completedQty: 7988,
            productionDate: '2024-12-02',
            productionStatus: '生产完成',
            uploadStatus: '上传失败',
            batchNumber: 'B2024003',
            createTime: '2024-12-02 08:30:00'
        },
        {
            id: 4,
            orderNo: 'PO202412020001',
            productName: '复合维生素片',
            plannedQty: 6000,
            completedQty: 5995,
            productionDate: '2024-12-02',
            productionStatus: '生产完成',
            uploadStatus: '待上传',
            batchNumber: 'B2024004',
            createTime: '2024-12-02 10:00:00'
        },
        {
            id: 5,
            orderNo: 'PO202412020002',
            productName: '钙片',
            plannedQty: 4000,
            completedQty: 3998,
            productionDate: '2024-12-02',
            productionStatus: '生产完成',
            uploadStatus: '待上传',
            batchNumber: 'B2024005',
            createTime: '2024-12-02 11:00:00'
        }
    ];
    
    // 生成生产任务数据
    mockTaskData = [];
    for (let orderId = 1; orderId <= 5; orderId++) {
        const order = mockOrders[orderId - 1];
        const dataCount = Math.min(order.completedQty, 100); // 限制演示数据量
        
        for (let i = 1; i <= dataCount; i++) {
            // 生成基础时间，每个产品间隔不同时间
            const baseTime = new Date(2024, 11, orderId, 10, 30, 15 + i * 2);
            
            // 为每层生成不同的采集时间（毫秒级）
            const level1CollectTime = new Date(baseTime.getTime() + Math.random() * 1000);
            const level2CollectTime = new Date(baseTime.getTime() + 1000 + Math.random() * 1000);
            const level3CollectTime = new Date(baseTime.getTime() + 2000 + Math.random() * 1000);
            const assignTime = new Date(baseTime.getTime() + 3000 + Math.random() * 1000);
            
            mockTaskData.push({
                id: (orderId - 1) * 1000 + i,
                orderId: orderId,
                orderNo: order.orderNo,
                level1Code: `L1_${orderId.toString().padStart(3, '0')}_${i.toString().padStart(4, '0')}`,
                level1CollectTime: level1CollectTime.toLocaleString('zh-CN', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                }) + '.' + level1CollectTime.getMilliseconds().toString().padStart(3, '0'),
                level2Code: `L2_${orderId.toString().padStart(3, '0')}_${Math.ceil(i / 6).toString().padStart(4, '0')}`,
                level2CollectTime: level2CollectTime.toLocaleString('zh-CN', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                }) + '.' + level2CollectTime.getMilliseconds().toString().padStart(3, '0'),
                level3Code: `L3_${orderId.toString().padStart(3, '0')}_${Math.ceil(i / 36).toString().padStart(4, '0')}`,
                level3CollectTime: level3CollectTime.toLocaleString('zh-CN', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                }) + '.' + level3CollectTime.getMilliseconds().toString().padStart(3, '0'),
                level4Code: `L4_${orderId.toString().padStart(3, '0')}_${Math.ceil(i / 216).toString().padStart(4, '0')}`,
                assignTime: assignTime.toLocaleString('zh-CN', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                }) + '.' + assignTime.getMilliseconds().toString().padStart(3, '0')
            });
        }
    }
    
    console.log('生成模拟数据:', mockOrders.length, '个订单,', mockTaskData.length, '条任务数据');
}

// 加载订单列表
function loadOrderList() {
    console.log('加载订单列表...');
    applyOrderFilters();
    renderOrderList();
}

function applyOrderFilters() {
    // 这里可以添加筛选逻辑
    filteredOrders = [...mockOrders];
    totalOrders = filteredOrders.length;
    console.log('筛选后订单数量:', totalOrders);
}

function renderOrderList() {
    const tbody = document.getElementById('orderTableBody');
    
    if (!tbody) {
        console.error('订单表格tbody未找到');
        return;
    }
    
    // 清空表格
    tbody.innerHTML = '';
    
    if (filteredOrders.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="empty-state">
                    <div class="empty-icon">📋</div>
                    <div class="empty-text">暂无生产订单数据</div>
                </td>
            </tr>
        `;
        return;
    }
    
    // 渲染数据行
    filteredOrders.forEach(order => {
        const row = createOrderRow(order);
        tbody.appendChild(row);
    });
}

function createOrderRow(order) {
    const row = document.createElement('tr');
    row.dataset.orderId = order.id;
    
    // 添加点击事件
    row.addEventListener('click', function() {
        selectOrder(order.id);
    });
    
    row.innerHTML = `
        <td>
            <a href="javascript:void(0)" class="order-link" onclick="selectOrder(${order.id})">
                ${order.orderNo}
            </a>
        </td>
        <td>${order.productName}</td>
        <td>${formatNumber(order.plannedQty)}</td>
        <td>${formatNumber(order.completedQty)}</td>
        <td>${order.productionDate}</td>
        <td>${createProductionStatusTag(order.productionStatus)}</td>
        <td>${createUploadStatusTag(order.uploadStatus)}</td>
        <td>
            <a href="javascript:void(0)" class="sync-link" onclick="syncOrder(${order.id})" title="同步上传">数据上传</a>
        </td>
    `;
    
    return row;
}

function createProductionStatusTag(status) {
    let className = 'status-tag';
    
    switch(status) {
        case '生产完成':
            className += ' production-completed';
            break;
        default:
            className += ' production-completed';
    }
    
    return `<span class="${className}">${status}</span>`;
}

function createUploadStatusTag(status) {
    let className = 'status-tag';
    
    switch(status) {
        case '待上传':
            className += ' upload-pending';
            break;
        case '上传中':
            className += ' upload-uploading';
            break;
        case '上传完成':
            className += ' upload-completed';
            break;
        case '上传失败':
            className += ' upload-failed';
            break;
        default:
            className += ' upload-pending';
    }
    
    return `<span class="${className}">${status}</span>`;
}

// 选择订单
function selectOrder(orderId) {
    // 移除之前的选中状态
    document.querySelectorAll('#orderTable tbody tr').forEach(row => {
        row.classList.remove('selected');
    });
    
    // 添加新的选中状态
    const selectedRow = document.querySelector(`#orderTable tbody tr[data-order-id="${orderId}"]`);
    if (selectedRow) {
        selectedRow.classList.add('selected');
    }
    
    selectedOrder = mockOrders.find(order => order.id === orderId);
    
    if (selectedOrder) {
        // 更新选中订单信息
        const selectedOrderInfo = document.getElementById('selectedOrderInfo');
        if (selectedOrderInfo) {
            selectedOrderInfo.textContent = `已选择订单：${selectedOrder.orderNo} - ${selectedOrder.productName}`;
        }
        
        // 加载任务数据
        loadTaskData(orderId);
    }
}

// 加载任务数据
function loadTaskData(orderId) {
    console.log('加载任务数据，订单ID:', orderId);
    
    // 筛选当前订单的任务数据
    filteredTaskData = mockTaskData.filter(data => data.orderId === orderId);
    totalTaskData = filteredTaskData.length;
    currentDataPage = 1;
    
    renderTaskData();
    renderDataPagination();
}

function renderTaskData() {
    const tbody = document.getElementById('taskDataTableBody');
    const totalCountSpan = document.getElementById('totalDataCount');
    
    if (!tbody) {
        console.error('任务数据表格tbody未找到');
        return;
    }
    
    // 更新总数显示
    if (totalCountSpan) {
        totalCountSpan.textContent = totalTaskData;
    }
    
    // 计算当前页数据
    const startIndex = (currentDataPage - 1) * dataPageSize;
    const endIndex = Math.min(startIndex + dataPageSize, totalTaskData);
    const currentPageData = filteredTaskData.slice(startIndex, endIndex);
    
    // 清空表格
    tbody.innerHTML = '';
    
    if (currentPageData.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="empty-state">
                    <div class="empty-icon">📄</div>
                    <div class="empty-text">暂无任务数据</div>
                </td>
            </tr>
        `;
        return;
    }
    
    // 渲染数据行
    currentPageData.forEach(data => {
        const row = createTaskDataRow(data);
        tbody.appendChild(row);
    });
}

function createTaskDataRow(data) {
    const row = document.createElement('tr');
    row.dataset.dataId = data.id;
    
    row.innerHTML = `
        <td><span class="code-value">${data.level1Code}</span></td>
        <td>${data.level1CollectTime}</td>
        <td><span class="code-value">${data.level2Code}</span></td>
        <td>${data.level2CollectTime}</td>
        <td><span class="code-value">${data.level3Code}</span></td>
        <td>${data.level3CollectTime}</td>
        <td><span class="code-value">${data.level4Code}</span></td>
        <td>${data.assignTime}</td>
    `;
    
    return row;
}

// 数据分页功能
function renderDataPagination() {
    const paginationControls = document.getElementById('dataPaginationControls');
    if (!paginationControls) return;
    
    const totalPages = Math.ceil(totalTaskData / dataPageSize);
    
    if (totalPages <= 1) {
        paginationControls.innerHTML = '';
        return;
    }
    
    let paginationHTML = '';
    
    // 首页按钮
    paginationHTML += `<button class="pagination-btn ${currentDataPage === 1 ? 'disabled' : ''}" ${currentDataPage === 1 ? 'disabled' : ''} onclick="goToDataPage(1)">首页</button>`;
    
    // 上一页按钮
    paginationHTML += `<button class="pagination-btn ${currentDataPage === 1 ? 'disabled' : ''}" ${currentDataPage === 1 ? 'disabled' : ''} onclick="goToDataPage(${currentDataPage - 1})">上一页</button>`;
    
    // 页码按钮
    const startPage = Math.max(1, currentDataPage - 2);
    const endPage = Math.min(totalPages, currentDataPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        paginationHTML += `<button class="pagination-btn ${i === currentDataPage ? 'active' : ''}" onclick="goToDataPage(${i})">${i}</button>`;
    }
    
    // 下一页按钮
    paginationHTML += `<button class="pagination-btn ${currentDataPage === totalPages ? 'disabled' : ''}" ${currentDataPage === totalPages ? 'disabled' : ''} onclick="goToDataPage(${currentDataPage + 1})">下一页</button>`;
    
    // 末页按钮
    paginationHTML += `<button class="pagination-btn ${currentDataPage === totalPages ? 'disabled' : ''}" ${currentDataPage === totalPages ? 'disabled' : ''} onclick="goToDataPage(${totalPages})">末页</button>`;
    
    paginationControls.innerHTML = paginationHTML;
}

function goToDataPage(page) {
    if (page < 1 || page > Math.ceil(totalTaskData / dataPageSize)) return;
    currentDataPage = page;
    renderTaskData();
    renderDataPagination();
}

function handleDataPageSizeChange() {
    const dataPageSizeSelect = document.getElementById('dataPageSizeSelect');
    dataPageSize = parseInt(dataPageSizeSelect.value);
    currentDataPage = 1;
    renderTaskData();
    renderDataPagination();
}

// 查询码功能
function handleCodeSearch() {
    const keyword = document.getElementById('codeSearch')?.value.trim();
    if (keyword) {
        searchCodeInData(keyword);
    }
}

function searchCode() {
    const keyword = document.getElementById('codeSearch')?.value.trim();
    
    if (!keyword) {
        showMessage('请输入查询码', 'warning');
        return;
    }
    
    searchCodeInData(keyword);
}

function searchCodeInData(keyword) {
    console.log('查询码:', keyword);
    
    // 在所有任务数据中搜索
    const results = mockTaskData.filter(data => 
        data.level1Code.includes(keyword) ||
        data.level2Code.includes(keyword) ||
        data.level3Code.includes(keyword) ||
        data.level4Code.includes(keyword)
    );
    
    if (results.length > 0) {
        // 显示搜索结果
        filteredTaskData = results;
        totalTaskData = results.length;
        currentDataPage = 1;
        
        // 更新选中订单信息
        const selectedOrderInfo = document.getElementById('selectedOrderInfo');
        if (selectedOrderInfo) {
            selectedOrderInfo.textContent = `查询码 "${keyword}" 的搜索结果`;
        }
        
        renderTaskData();
        renderDataPagination();
        
        // 高亮显示匹配的码值
        highlightCodeValues(keyword);
        
        showMessage(`找到 ${results.length} 条匹配的数据`, 'success');
    } else {
        showMessage('未找到相关数据', 'warning');
    }
}

function highlightCodeValues(keyword) {
    // 高亮显示匹配的码值
    setTimeout(() => {
        document.querySelectorAll('.code-value').forEach(element => {
            if (element.textContent.includes(keyword)) {
                element.classList.add('code-highlight');
            } else {
                element.classList.remove('code-highlight');
            }
        });
    }, 100);
}

// 同步订单功能
function syncOrder(orderId) {
    const order = mockOrders.find(o => o.id === orderId);
    
    if (!order) {
        showMessage('订单不存在', 'error');
        return;
    }
    
    if (order.uploadStatus === '上传完成') {
        showMessage('该订单已经上传完成', 'info');
        return;
    }
    
    if (uploadProgress.isUploading) {
        showMessage('正在上传中，请等待当前上传完成', 'warning');
        return;
    }
    
    // 选中该订单
    selectOrder(orderId);
    
    // 开始上传
    startUpload(order);
}

// 一键上传功能 - 批量上传所有订单
function uploadOrder() {
    if (uploadProgress.isUploading) {
        showMessage('正在上传中，请等待当前上传完成', 'warning');
        return;
    }
    
    // 获取所有未上传完成的订单
    const pendingOrders = mockOrders.filter(order => order.uploadStatus !== '上传完成');
    
    if (pendingOrders.length === 0) {
        showMessage('所有订单都已上传完成', 'info');
        return;
    }
    
    // 开始批量上传
    startBatchUpload(pendingOrders);
}

// 批量上传功能
function startBatchUpload(orders) {
    uploadProgress.isUploading = true;
    uploadProgress.batchOrders = orders;
    uploadProgress.currentBatchIndex = 0;
    uploadProgress.totalBatchCount = orders.length;
    uploadProgress.progress = 0;
    uploadProgress.status = 'uploading';
    
    // 显示批量上传进度对话框
    showBatchUploadProgress(orders);
    
    // 开始上传第一个订单
    uploadNextOrder();
}

// 上传下一个订单
function uploadNextOrder() {
    if (!uploadProgress.isUploading || uploadProgress.currentBatchIndex >= uploadProgress.totalBatchCount) {
        // 批量上传完成
        finishBatchUpload();
        return;
    }
    
    const currentOrder = uploadProgress.batchOrders[uploadProgress.currentBatchIndex];
    uploadProgress.currentOrder = currentOrder;
    
    // 更新订单状态
    currentOrder.uploadStatus = '上传中';
    renderOrderList();
    
    // 更新进度信息
    updateBatchUploadProgress();
    
    // 模拟上传过程
    simulateUpload(currentOrder, true);
}

function startUpload(order) {
    uploadProgress.isUploading = true;
    uploadProgress.currentOrder = order;
    uploadProgress.progress = 0;
    uploadProgress.status = 'uploading';
    
    // 更新订单状态
    order.uploadStatus = '上传中';
    renderOrderList();
    
    // 显示上传进度对话框
    showUploadProgress(order);
    
    // 模拟上传过程
    simulateUpload(order);
}

// 显示批量上传进度
function showBatchUploadProgress(orders) {
    const modal = document.getElementById('uploadProgressModal');
    const orderNoSpan = document.getElementById('uploadOrderNo');
    const dataCountSpan = document.getElementById('uploadDataCount');
    const cancelBtn = document.getElementById('cancelUploadBtn');
    const closeBtn = document.getElementById('closeUploadBtn');
    
    if (orderNoSpan) {
        orderNoSpan.textContent = `批量上传 (${orders.length}个订单)`;
    }
    
    if (dataCountSpan) {
        const totalDataCount = orders.reduce((sum, order) => {
            return sum + mockTaskData.filter(data => data.orderId === order.id).length;
        }, 0);
        dataCountSpan.textContent = totalDataCount;
    }
    
    // 重置进度
    updateUploadProgress(0, '准备批量上传...');
    
    // 清空日志
    const uploadLog = document.getElementById('uploadLog');
    if (uploadLog) {
        uploadLog.innerHTML = '';
    }
    
    // 显示取消按钮，隐藏关闭按钮
    if (cancelBtn) cancelBtn.style.display = 'inline-block';
    if (closeBtn) closeBtn.style.display = 'none';
    
    modal.style.display = 'flex';
}

function showUploadProgress(order) {
    const modal = document.getElementById('uploadProgressModal');
    const orderNoSpan = document.getElementById('uploadOrderNo');
    const dataCountSpan = document.getElementById('uploadDataCount');
    const cancelBtn = document.getElementById('cancelUploadBtn');
    const closeBtn = document.getElementById('closeUploadBtn');
    
    if (orderNoSpan) {
        orderNoSpan.textContent = order.orderNo;
    }
    
    if (dataCountSpan) {
        const dataCount = mockTaskData.filter(data => data.orderId === order.id).length;
        dataCountSpan.textContent = dataCount;
    }
    
    // 重置进度
    updateUploadProgress(0, '准备中...');
    
    // 清空日志
    const uploadLog = document.getElementById('uploadLog');
    if (uploadLog) {
        uploadLog.innerHTML = '';
    }
    
    // 显示取消按钮，隐藏关闭按钮
    if (cancelBtn) cancelBtn.style.display = 'inline-block';
    if (closeBtn) closeBtn.style.display = 'none';
    
    modal.style.display = 'flex';
}

function simulateUpload(order, isBatch = false) {
    const steps = [
        { progress: 10, status: '数据校验中...', delay: 300 },
        { progress: 25, status: '数据打包中...', delay: 400 },
        { progress: 40, status: '连接服务器...', delay: 300 },
        { progress: 60, status: '上传数据中...', delay: 600 },
        { progress: 80, status: '验证数据完整性...', delay: 300 },
        { progress: 95, status: '更新状态...', delay: 200 },
        { progress: 100, status: '上传完成', delay: 200 }
    ];
    
    let currentStep = 0;
    
    function executeStep() {
        if (!uploadProgress.isUploading) {
            return; // 上传已取消
        }
        
        if (currentStep < steps.length) {
            const step = steps[currentStep];
            
            if (isBatch) {
                // 批量上传时显示当前订单的进度
                const orderProgress = `订单 ${order.orderNo}: ${step.status}`;
                updateUploadProgress(step.progress, orderProgress);
                addUploadLog(`[${new Date().toLocaleTimeString()}] ${orderProgress}`, 'info');
            } else {
                updateUploadProgress(step.progress, step.status);
                addUploadLog(`[${new Date().toLocaleTimeString()}] ${step.status}`, 'info');
            }
            
            currentStep++;
            setTimeout(executeStep, step.delay);
        } else {
            // 单个订单上传完成
            finishUpload(order, true, isBatch);
        }
    }
    
    executeStep();
}

function updateUploadProgress(progress, status) {
    const progressFill = document.getElementById('progressFill');
    const progressPercent = document.getElementById('progressPercent');
    const progressStatus = document.getElementById('progressStatus');
    
    if (progressFill) {
        progressFill.style.width = `${progress}%`;
    }
    
    if (progressPercent) {
        progressPercent.textContent = `${progress}%`;
    }
    
    if (progressStatus) {
        progressStatus.textContent = status;
    }
    
    uploadProgress.progress = progress;
}

function addUploadLog(message, type = 'info') {
    const uploadLog = document.getElementById('uploadLog');
    if (uploadLog) {
        const logEntry = document.createElement('div');
        logEntry.className = `log-entry ${type}`;
        logEntry.textContent = message;
        uploadLog.appendChild(logEntry);
        uploadLog.scrollTop = uploadLog.scrollHeight;
    }
}

function finishUpload(order, success, isBatch = false) {
    if (success) {
        order.uploadStatus = '上传完成';
        addUploadLog(`[${new Date().toLocaleTimeString()}] 订单 ${order.orderNo} 上传成功完成`, 'success');
        if (!isBatch) {
            showMessage(`订单 ${order.orderNo} 上传成功`, 'success');
        }
    } else {
        order.uploadStatus = '上传失败';
        addUploadLog(`[${new Date().toLocaleTimeString()}] 订单 ${order.orderNo} 上传失败`, 'error');
        if (!isBatch) {
            showMessage(`订单 ${order.orderNo} 上传失败`, 'error');
        }
    }
    
    // 更新界面
    renderOrderList();
    
    if (isBatch) {
        // 批量上传：继续下一个订单
        uploadProgress.currentBatchIndex++;
        setTimeout(uploadNextOrder, 500); // 短暂延迟后继续下一个
    } else {
        // 单个上传：完成上传
        uploadProgress.isUploading = false;
        uploadProgress.status = success ? 'completed' : 'failed';
        
        // 更新按钮状态
        const cancelBtn = document.getElementById('cancelUploadBtn');
        const closeBtn = document.getElementById('closeUploadBtn');
        
        if (cancelBtn) cancelBtn.style.display = 'none';
        if (closeBtn) closeBtn.style.display = 'inline-block';
        
        // 更新状态栏
        const uploadStatus = document.getElementById('uploadStatus');
        if (uploadStatus) {
            uploadStatus.textContent = success ? '上传状态：完成' : '上传状态：失败';
        }
    }
}

// 更新批量上传进度
function updateBatchUploadProgress() {
    const currentIndex = uploadProgress.currentBatchIndex;
    const totalCount = uploadProgress.totalBatchCount;
    const overallProgress = Math.round((currentIndex / totalCount) * 100);
    
    const progressStatus = `正在上传第 ${currentIndex + 1} 个订单，共 ${totalCount} 个订单 (${overallProgress}%)`;
    updateUploadProgress(overallProgress, progressStatus);
}

// 完成批量上传
function finishBatchUpload() {
    uploadProgress.isUploading = false;
    uploadProgress.status = 'completed';
    
    const successCount = uploadProgress.batchOrders.filter(order => order.uploadStatus === '上传完成').length;
    const failCount = uploadProgress.totalBatchCount - successCount;
    
    addUploadLog(`[${new Date().toLocaleTimeString()}] 批量上传完成：成功 ${successCount} 个，失败 ${failCount} 个`, 'success');
    showMessage(`批量上传完成：成功 ${successCount} 个订单，失败 ${failCount} 个订单`, 'success');
    
    // 更新按钮状态
    const cancelBtn = document.getElementById('cancelUploadBtn');
    const closeBtn = document.getElementById('closeUploadBtn');
    
    if (cancelBtn) cancelBtn.style.display = 'none';
    if (closeBtn) closeBtn.style.display = 'inline-block';
    
    // 更新状态栏
    const uploadStatus = document.getElementById('uploadStatus');
    if (uploadStatus) {
        uploadStatus.textContent = `上传状态：批量完成 (${successCount}/${uploadProgress.totalBatchCount})`;
    }
    
    // 清理批量上传状态
    uploadProgress.batchOrders = null;
    uploadProgress.currentBatchIndex = 0;
    uploadProgress.totalBatchCount = 0;
}

function cancelUpload() {
    if (uploadProgress.isUploading) {
        uploadProgress.isUploading = false;
        
        if (uploadProgress.batchOrders) {
            // 取消批量上传
            uploadProgress.batchOrders.forEach(order => {
                if (order.uploadStatus === '上传中') {
                    order.uploadStatus = '待上传';
                }
            });
            addUploadLog(`[${new Date().toLocaleTimeString()}] 批量上传已取消`, 'warning');
            showMessage('批量上传已取消', 'info');
            
            // 清理批量上传状态
            uploadProgress.batchOrders = null;
            uploadProgress.currentBatchIndex = 0;
            uploadProgress.totalBatchCount = 0;
        } else if (uploadProgress.currentOrder) {
            // 取消单个上传
            uploadProgress.currentOrder.uploadStatus = '待上传';
            addUploadLog(`[${new Date().toLocaleTimeString()}] 上传已取消`, 'warning');
            showMessage('上传已取消', 'info');
        }
        
        renderOrderList();
        
        // 更新按钮状态
        const cancelBtn = document.getElementById('cancelUploadBtn');
        const closeBtn = document.getElementById('closeUploadBtn');
        
        if (cancelBtn) cancelBtn.style.display = 'none';
        if (closeBtn) closeBtn.style.display = 'inline-block';
    }
}

function closeUploadProgress() {
    const modal = document.getElementById('uploadProgressModal');
    modal.style.display = 'none';
    
    uploadProgress.currentOrder = null;
    uploadProgress.batchOrders = null;
    uploadProgress.currentBatchIndex = 0;
    uploadProgress.totalBatchCount = 0;
    uploadProgress.progress = 0;
    uploadProgress.status = 'ready';
    
    // 更新状态栏
    const uploadStatus = document.getElementById('uploadStatus');
    if (uploadStatus) {
        uploadStatus.textContent = '上传状态：就绪';
    }
}

// 码替换功能
function showCodeReplaceModal() {
    const modal = document.getElementById('codeReplaceModal');
    
    // 清空表单
    document.getElementById('oldCodeValue').value = '';
    document.getElementById('newCodeValue').value = '';
    document.getElementById('replaceReason').value = '';
    
    modal.style.display = 'flex';
}

function closeCodeReplaceModal() {
    const modal = document.getElementById('codeReplaceModal');
    modal.style.display = 'none';
}

function confirmCodeReplace() {
    const oldCode = document.getElementById('oldCodeValue').value.trim();
    const newCode = document.getElementById('newCodeValue').value.trim();
    const reason = document.getElementById('replaceReason').value.trim();
    
    if (!oldCode || !newCode) {
        showMessage('请输入原码值和新码值', 'warning');
        return;
    }
    
    if (oldCode === newCode) {
        showMessage('新码值不能与原码值相同', 'warning');
        return;
    }
    
    // 查找要替换的码
    let foundData = null;
    let codeField = '';
    
    for (let data of mockTaskData) {
        if (data.level1Code === oldCode) {
            foundData = data;
            codeField = 'level1Code';
            break;
        } else if (data.level2Code === oldCode) {
            foundData = data;
            codeField = 'level2Code';
            break;
        } else if (data.level3Code === oldCode) {
            foundData = data;
            codeField = 'level3Code';
            break;
        } else if (data.level4Code === oldCode) {
            foundData = data;
            codeField = 'level4Code';
            break;
        }
    }
    
    if (!foundData) {
        showMessage('未找到指定的码值', 'warning');
        return;
    }
    
    // 检查新码值是否已存在
    const newCodeExists = mockTaskData.some(data => 
        data.level1Code === newCode ||
        data.level2Code === newCode ||
        data.level3Code === newCode ||
        data.level4Code === newCode
    );
    
    if (newCodeExists) {
        showMessage('新码值已存在，请使用其他码值', 'warning');
        return;
    }
    
    // 执行替换
    foundData[codeField] = newCode;
    
    // 刷新显示
    if (selectedOrder && foundData.orderId === selectedOrder.id) {
        renderTaskData();
    }
    
    closeCodeReplaceModal();
    showMessage(`码值替换成功：${oldCode} → ${newCode}`, 'success');
    
    console.log('码替换记录:', { oldCode, newCode, reason, timestamp: new Date().toLocaleString() });
}

// 码删除功能
function showCodeDeleteModal() {
    const modal = document.getElementById('codeDeleteModal');
    const codeInfoSection = document.getElementById('codeInfoSection');
    
    // 清空表单
    document.getElementById('deleteCodeValue').value = '';
    document.getElementById('deleteReason').value = '';
    
    // 隐藏码关联信息
    codeInfoSection.style.display = 'none';
    
    modal.style.display = 'flex';
    
    // 监听码值输入变化
    const deleteCodeInput = document.getElementById('deleteCodeValue');
    deleteCodeInput.addEventListener('input', function() {
        const codeValue = this.value.trim();
        if (codeValue) {
            showCodeRelations(codeValue);
        } else {
            codeInfoSection.style.display = 'none';
        }
    });
}

function showCodeRelations(codeValue) {
    const codeInfoSection = document.getElementById('codeInfoSection');
    const codeRelations = document.getElementById('codeRelations');
    
    // 查找码关联信息
    const relatedData = mockTaskData.filter(data => 
        data.level1Code === codeValue ||
        data.level2Code === codeValue ||
        data.level3Code === codeValue ||
        data.level4Code === codeValue
    );
    
    if (relatedData.length > 0) {
        let relationsHTML = '';
        
        relatedData.forEach(data => {
            relationsHTML += `
                <div class="code-relation-item">
                    <strong>订单：</strong>${data.orderNo}<br>
                    <strong>第一层：</strong>${data.level1Code}<br>
                    <strong>第二层：</strong>${data.level2Code}<br>
                    <strong>第三层：</strong>${data.level3Code}<br>
                    <strong>第四层：</strong>${data.level4Code}<br>
                    <strong>赋码时间：</strong>${data.assignTime}
                </div>
            `;
        });
        
        codeRelations.innerHTML = relationsHTML;
        codeInfoSection.style.display = 'block';
    } else {
        codeInfoSection.style.display = 'none';
    }
}

function closeCodeDeleteModal() {
    const modal = document.getElementById('codeDeleteModal');
    modal.style.display = 'none';
}

function confirmCodeDelete() {
    const deleteCode = document.getElementById('deleteCodeValue').value.trim();
    const reason = document.getElementById('deleteReason').value.trim();
    
    if (!deleteCode) {
        showMessage('请输入要删除的码值', 'warning');
        return;
    }
    
    // 查找并删除相关数据
    let deletedCount = 0;
    let affectedOrders = new Set();
    
    mockTaskData = mockTaskData.filter(data => {
        const shouldDelete = data.level1Code === deleteCode ||
                           data.level2Code === deleteCode ||
                           data.level3Code === deleteCode ||
                           data.level4Code === deleteCode;
        
        if (shouldDelete) {
            deletedCount++;
            affectedOrders.add(data.orderNo);
        }
        
        return !shouldDelete;
    });
    
    if (deletedCount > 0) {
        // 刷新显示
        if (selectedOrder) {
            loadTaskData(selectedOrder.id);
        }
        
        closeCodeDeleteModal();
        showMessage(`码删除成功，共删除 ${deletedCount} 条相关数据`, 'success');
        
        console.log('码删除记录:', { 
            deleteCode, 
            reason, 
            deletedCount, 
            affectedOrders: Array.from(affectedOrders),
            timestamp: new Date().toLocaleString() 
        });
    } else {
        showMessage('未找到指定的码值', 'warning');
    }
}

// 工具函数
function formatNumber(num) {
    return num.toLocaleString('zh-CN');
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
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

// 全局函数导出
window.uploadOrder = uploadOrder;
window.syncOrder = syncOrder;
window.showCodeReplaceModal = showCodeReplaceModal;
window.showCodeDeleteModal = showCodeDeleteModal;
window.searchCode = searchCode;
window.selectOrder = selectOrder;
window.handleDataPageSizeChange = handleDataPageSizeChange;
window.goToDataPage = goToDataPage;
window.closeCodeReplaceModal = closeCodeReplaceModal;
window.confirmCodeReplace = confirmCodeReplace;
window.closeCodeDeleteModal = closeCodeDeleteModal;
window.confirmCodeDelete = confirmCodeDelete;
window.cancelUpload = cancelUpload;
window.closeUploadProgress = closeUploadProgress;

console.log('数据上传管理脚本加载完成'); 