// 任务列表管理页面JavaScript功能

// 全局变量 - 使用taskList前缀避免与其他页面冲突
let taskListCurrentPage = 1;
let taskListPageSize = 20;
let taskListTotalTasks = 0;
let taskListFilteredTasks = [];
let taskListSelectedTasks = [];
let taskListAutoRefreshTimer = null;
let taskListMockTasks = [];

// 页面初始化 - 延迟执行以避免与desktop-app.js冲突
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, initializing task list page...');
    // 延迟执行以确保desktop-app.js先执行
    setTimeout(() => {
        initializePage();
    }, 100);
});

// 备用初始化 - 如果DOMContentLoaded没有正常工作
window.addEventListener('load', function() {
    console.log('Window loaded, backup initialization...');
    if (mockTasks.length === 0) {
        setTimeout(() => {
            initializePage();
        }, 200);
    }
});

// 立即执行测试
console.log('task-list.js loaded successfully');

function initializePage() {
    console.log('Step 1: Updating time display...');
    // 更新时间显示 - 使用desktop-app.js中的updateTime函数
    // updateTime();
    // setInterval(updateTime, 1000);
    
    console.log('Step 2: Initializing event listeners...');
    // 初始化事件监听
    initEventListeners();
    
    console.log('Step 3: Generating mock data...');
    // 生成模拟数据
    generateMockData();
    console.log('Generated tasks:', mockTasks.length);
    
    console.log('Step 4: Loading task list...');
    // 加载任务列表
    loadTaskList();
    
    console.log('Step 5: Starting auto refresh...');
    // 启动自动刷新
    startAutoRefresh();
    
    console.log('Initialization complete!');
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

function initEventListeners() {
    // 搜索和筛选
    const taskNumberSearch = document.getElementById('taskNumberSearch');
    const batchNumberSearch = document.getElementById('batchNumberSearch');
    const startDate = document.getElementById('startDate');
    const endDate = document.getElementById('endDate');
    
    if (taskNumberSearch) {
        taskNumberSearch.addEventListener('input', debounce(handleSearch, 300));
    }
    
    if (batchNumberSearch) {
        batchNumberSearch.addEventListener('input', debounce(handleSearch, 300));
    }
    
    if (startDate) {
        startDate.addEventListener('change', handleFilter);
    }
    
    if (endDate) {
        endDate.addEventListener('change', handleFilter);
    }
    
    // 全选复选框
    const selectAll = document.getElementById('selectAll');
    if (selectAll) {
        selectAll.addEventListener('change', handleSelectAll);
    }
    
    // 分页大小选择
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', handlePageSizeChange);
    }
    
    // 模态框关闭按钮
    const closeTaskDetailBtn = document.getElementById('closeTaskDetailBtn');
    const closeAdvancedFilterBtn = document.getElementById('closeAdvancedFilterBtn');
    
    if (closeTaskDetailBtn) {
        closeTaskDetailBtn.addEventListener('click', closeTaskDetail);
    }
    
    if (closeAdvancedFilterBtn) {
        closeAdvancedFilterBtn.addEventListener('click', closeAdvancedFilter);
    }
    
    // 详情标签页
    const detailTabs = document.querySelectorAll('.detail-tabs .tab-btn');
    detailTabs.forEach(tab => {
        tab.addEventListener('click', handleDetailTabClick);
    });
}

// 生成模拟数据
function generateMockData() {
    const statuses = ['待开始', '进行中', '已暂停', '已完成', '已停止', '异常'];
    const products = [
        { code: 'P001', name: '维生素C片', ratio: '1:100', unit: '瓶' },
        { code: 'P002', name: '感冒灵颗粒', ratio: '1:50', unit: '盒' },
        { code: 'P003', name: '阿莫西林胶囊', ratio: '1:200', unit: '瓶' },
        { code: 'P004', name: '板蓝根颗粒', ratio: '1:80', unit: '盒' },
        { code: 'P005', name: '复合维生素片', ratio: '1:120', unit: '瓶' }
    ];
    
    mockTasks = [];
    
    // 生成5个固定的示例数据
    const sampleTasks = [
        {
            id: 1,
            productionOrder: 'PO202412010001',
            productCode: 'P001',
            productName: '维生素C片',
            plannedQty: 5000,
            completedQty: 3200,
            packagingRatio: '1:100',
            unit: '瓶',
            batchNumber: 'B2024001',
            productionDate: '2024-12-01',
            expiryDate: '2026-12-01',
            orderDate: '2024-11-28',
            status: '进行中',
            remarks: '优先生产订单',
            progress: Math.round((3200 / 5000) * 100)
        },
        {
            id: 2,
            productionOrder: 'PO202412010002',
            productCode: 'P002',
            productName: '感冒灵颗粒',
            plannedQty: 3000,
            completedQty: 3000,
            packagingRatio: '1:50',
            unit: '盒',
            batchNumber: 'B2024002',
            productionDate: '2024-12-01',
            expiryDate: '2026-12-01',
            orderDate: '2024-11-29',
            status: '已完成',
            remarks: '质量检验合格',
            progress: 100
        },
        {
            id: 3,
            productionOrder: 'PO202412010003',
            productCode: 'P003',
            productName: '阿莫西林胶囊',
            plannedQty: 8000,
            completedQty: 0,
            packagingRatio: '1:200',
            unit: '瓶',
            batchNumber: 'B2024003',
            productionDate: '2024-12-02',
            expiryDate: '2026-12-02',
            orderDate: '2024-11-30',
            status: '待开始',
            remarks: '等待原料到货',
            progress: 0
        },
        {
            id: 4,
            productionOrder: 'PO202412010004',
            productCode: 'P004',
            productName: '板蓝根颗粒',
            plannedQty: 2500,
            completedQty: 1200,
            packagingRatio: '1:80',
            unit: '盒',
            batchNumber: 'B2024004',
            productionDate: '2024-12-01',
            expiryDate: '2026-12-01',
            orderDate: '2024-11-27',
            status: '已暂停',
            remarks: '设备维护中',
            progress: Math.round((1200 / 2500) * 100)
        },
        {
            id: 5,
            productionOrder: 'PO202412010005',
            productCode: 'P005',
            productName: '复合维生素片',
            plannedQty: 4500,
            completedQty: 800,
            packagingRatio: '1:120',
            unit: '瓶',
            batchNumber: 'B2024005',
            productionDate: '2024-12-01',
            expiryDate: '2026-12-01',
            orderDate: '2024-11-26',
            status: '异常',
            remarks: '包装材料不足',
            progress: Math.round((800 / 4500) * 100)
        }
    ];
    
    taskListMockTasks = [...sampleTasks];
    console.log('Mock data generated:', taskListMockTasks);
}

// 加载任务列表
function loadTaskList() {
    console.log('Loading task list...');
    console.log('taskListMockTasks before filtering:', taskListMockTasks.length);
    
    // 直接应用过滤器，不使用异步
    applyFilters();
    console.log('Filtered tasks after filtering:', taskListFilteredTasks.length);
    
    console.log('Rendering task list...');
    renderTaskList();
    
    console.log('Rendering pagination...');
    renderPagination();
    
    console.log('Task list loading complete!');
}

function applyFilters() {
    console.log('applyFilters called, taskListMockTasks:', taskListMockTasks.length);
    
    const taskNumberSearch = document.getElementById('taskNumberSearch')?.value.toLowerCase() || '';
    const batchNumberSearch = document.getElementById('batchNumberSearch')?.value.toLowerCase() || '';
    const startDate = document.getElementById('startDate')?.value || '';
    const endDate = document.getElementById('endDate')?.value || '';
    
    console.log('Filter values:', { taskNumberSearch, batchNumberSearch, startDate, endDate });
    
    taskListFilteredTasks = taskListMockTasks.filter(task => {
        // 任务单号搜索
        if (taskNumberSearch && !task.productionOrder.toLowerCase().includes(taskNumberSearch)) {
            return false;
        }
        
        // 生产批次搜索
        if (batchNumberSearch && !task.batchNumber.toLowerCase().includes(batchNumberSearch)) {
            return false;
        }
        
        // 日期过滤
        if (startDate && task.productionDate < startDate) {
            return false;
        }
        
        if (endDate && task.productionDate > endDate) {
            return false;
        }
        
        return true;
    });
    
    taskListTotalTasks = taskListFilteredTasks.length;
    console.log('Filter result: taskListTotalTasks =', taskListTotalTasks);
}

function renderTaskList() {
    console.log('renderTaskList called');
    console.log('taskListMockTasks:', taskListMockTasks);
    console.log('taskListFilteredTasks:', taskListFilteredTasks);
    console.log('taskListTotalTasks:', taskListTotalTasks);
    
    const tbody = document.getElementById('taskTableBody');
    const totalCountSpan = document.getElementById('totalCount');
    
    if (!tbody) {
        console.log('tbody not found!');
        return;
    }
    
    // 更新总数显示
    if (totalCountSpan) {
        totalCountSpan.textContent = taskListTotalTasks;
    }
    
    // 计算当前页数据
    const startIndex = (taskListCurrentPage - 1) * taskListPageSize;
    const endIndex = Math.min(startIndex + taskListPageSize, taskListTotalTasks);
    const currentPageTasks = taskListFilteredTasks.slice(startIndex, endIndex);
    
    // 清空表格
    tbody.innerHTML = '';
    
    if (currentPageTasks.length === 0) {
        console.log('No tasks to display, showing empty state');
        tbody.innerHTML = `
            <tr>
                <td colspan="15" class="empty-state">
                    <div class="empty-icon">📋</div>
                    <div class="empty-text">暂无任务数据</div>
                    <div class="empty-action">
                        <button class="btn btn-primary" onclick="showAddTaskModal()">添加任务</button>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    console.log('Rendering', currentPageTasks.length, 'tasks');
    
    // 渲染数据行
    currentPageTasks.forEach(task => {
        const row = createTaskRow(task);
        tbody.appendChild(row);
    });
}

function createTaskRow(task) {
    const row = document.createElement('tr');
    row.dataset.taskId = task.id;
    
    // 根据状态添加行样式
    if (task.status === '异常') {
        row.classList.add('error-row');
    }
    
    row.innerHTML = `
        <td>
            <input type="checkbox" class="task-checkbox" value="${task.id}" onchange="handleTaskSelect(this)">
        </td>
        <td>
            <a href="javascript:void(0)" onclick="showTaskDetail(${task.id})" class="task-link">
                ${task.productionOrder}
            </a>
        </td>
        <td>${task.productCode}</td>
        <td>${task.productName}</td>
        <td>${task.plannedQty.toLocaleString()}</td>
        <td>${task.completedQty.toLocaleString()}</td>
        <td>${task.packagingRatio}</td>
        <td>${task.unit}</td>
        <td>${task.batchNumber}</td>
        <td>${task.productionDate}</td>
        <td>${task.expiryDate}</td>
        <td>${task.orderDate}</td>
        <td>
            <span class="status-tag ${getStatusClass(task.status)}">
                ${getStatusIcon(task.status)} ${task.status}
            </span>
        </td>
        <td>${task.remarks || '-'}</td>
        <td>
            <div class="action-buttons">
                ${createActionButtons(task)}
            </div>
        </td>
    `;
    
    return row;
}

function createActionButtons(task) {
    let buttons = [];
    
    // 修改按钮（所有状态都可以修改）
    buttons.push(`<button class="action-btn edit" title="修改任务" onclick="editTask(${task.id})">✏️</button>`);
    
    // 移除按钮（所有状态都可以移除）
    buttons.push(`<button class="action-btn delete" title="移除任务" onclick="removeTask(${task.id})">🗑️</button>`);
    
    return buttons.join('');
}

// 辅助函数
function getProgressClass(progress) {
    if (progress < 30) return 'low';
    if (progress < 70) return 'medium';
    return 'high';
}

function getStatusClass(status) {
    const statusMap = {
        '待开始': 'pending',
        '进行中': 'running',
        '已暂停': 'paused',
        '已完成': 'completed',
        '已停止': 'stopped',
        '异常': 'error'
    };
    return statusMap[status] || 'pending';
}

function getStatusIcon(status) {
    const iconMap = {
        '待开始': '⏳',
        '进行中': '🔄',
        '已暂停': '⏸️',
        '已完成': '✅',
        '已停止': '⏹️',
        '异常': '❌'
    };
    return iconMap[status] || '⏳';
}

function getPriorityClass(priority) {
    const priorityMap = {
        '高': 'high',
        '中': 'medium',
        '低': 'low'
    };
    return priorityMap[priority] || 'medium';
}

function getPriorityIcon(priority) {
    const iconMap = {
        '高': '🔴',
        '中': '🟡',
        '低': '🟢'
    };
    return iconMap[priority] || '🟡';
}

// 分页功能
function renderPagination() {
    const paginationControls = document.getElementById('paginationControls');
    if (!paginationControls) return;
    
    const totalPages = Math.ceil(totalTasks / pageSize);
    
    if (totalPages <= 1) {
        paginationControls.innerHTML = '';
        return;
    }
    
    let paginationHTML = '';
    
    // 首页按钮
    paginationHTML += `<button ${currentPage === 1 ? 'disabled' : ''} onclick="goToPage(1)">首页</button>`;
    
    // 上一页按钮
    paginationHTML += `<button ${currentPage === 1 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})">上一页</button>`;
    
    // 页码按钮
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        paginationHTML += `<button class="${i === currentPage ? 'active' : ''}" onclick="goToPage(${i})">${i}</button>`;
    }
    
    // 下一页按钮
    paginationHTML += `<button ${currentPage === totalPages ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})">下一页</button>`;
    
    // 末页按钮
    paginationHTML += `<button ${currentPage === totalPages ? 'disabled' : ''} onclick="goToPage(${totalPages})">末页</button>`;
    
    paginationControls.innerHTML = paginationHTML;
}

function goToPage(page) {
    currentPage = page;
    renderTaskList();
    renderPagination();
}

function handlePageSizeChange() {
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    pageSize = parseInt(pageSizeSelect.value);
    currentPage = 1;
    renderTaskList();
    renderPagination();
}

// 搜索和筛选
function handleSearch() {
    currentPage = 1;
    loadTaskList();
}

function handleFilter() {
    currentPage = 1;
    loadTaskList();
}

// 防抖函数
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

// 选择功能
function handleSelectAll() {
    const selectAll = document.getElementById('selectAll');
    const checkboxes = document.querySelectorAll('.task-checkbox');
    
    checkboxes.forEach(checkbox => {
        checkbox.checked = selectAll.checked;
    });
    
    updateSelectedTasks();
}

function handleTaskSelect(checkbox) {
    updateSelectedTasks();
    
    // 更新全选状态
    const selectAll = document.getElementById('selectAll');
    const checkboxes = document.querySelectorAll('.task-checkbox');
    const checkedBoxes = document.querySelectorAll('.task-checkbox:checked');
    
    selectAll.checked = checkboxes.length === checkedBoxes.length;
    selectAll.indeterminate = checkedBoxes.length > 0 && checkedBoxes.length < checkboxes.length;
}

function updateSelectedTasks() {
    const checkedBoxes = document.querySelectorAll('.task-checkbox:checked');
    selectedTasks = Array.from(checkedBoxes).map(cb => parseInt(cb.value));
    
    const selectedCountSpan = document.getElementById('selectedCount');
    if (selectedCountSpan) {
        if (selectedTasks.length > 0) {
            selectedCountSpan.style.display = 'inline';
            selectedCountSpan.querySelector('span').textContent = selectedTasks.length;
        } else {
            selectedCountSpan.style.display = 'none';
        }
    }
}



// 任务操作功能
function showTaskDetail(taskId) {
    const task = mockTasks.find(t => t.id === taskId);
    if (!task) return;
    
    const modal = document.getElementById('taskDetailModal');
    const title = document.getElementById('taskDetailTitle');
    
    title.textContent = `任务详情 - ${task.productionOrder}`;
    
    // 显示基本信息标签页
    showTaskDetailTab('basic', task);
    
    modal.style.display = 'flex';
}

function closeTaskDetail() {
    const modal = document.getElementById('taskDetailModal');
    modal.style.display = 'none';
}

function handleDetailTabClick(event) {
    const tabName = event.target.dataset.tab;
    const taskId = getCurrentTaskId();
    const task = mockTasks.find(t => t.id === taskId);
    
    // 更新标签页状态
    document.querySelectorAll('.detail-tabs .tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');
    
    // 显示对应内容
    showTaskDetailTab(tabName, task);
}

function getCurrentTaskId() {
    // 从模态框标题中提取任务ID（简化实现）
    const title = document.getElementById('taskDetailTitle').textContent;
    const productionOrder = title.split(' - ')[1];
    const task = mockTasks.find(t => t.productionOrder === productionOrder);
    return task ? task.id : null;
}

function showTaskDetailTab(tabName, task) {
    const content = document.getElementById('taskDetailContent');
    
    switch (tabName) {
        case 'basic':
            content.innerHTML = createBasicInfoContent(task);
            break;
        case 'progress':
            content.innerHTML = createProgressContent(task);
            break;
        case 'details':
            content.innerHTML = createDetailsContent(task);
            break;
        case 'exceptions':
            content.innerHTML = createExceptionsContent(task);
            break;
        case 'logs':
            content.innerHTML = createLogsContent(task);
            break;
    }
}

function createBasicInfoContent(task) {
    return `
        <div class="tab-pane active">
            <div class="info-grid">
                <div class="info-card">
                    <h4>任务信息</h4>
                    <div class="info-item">
                        <span class="info-label">生产订单：</span>
                        <span class="info-value">${task.productionOrder}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">任务状态：</span>
                        <span class="info-value">
                            <span class="status-tag ${getStatusClass(task.status)}">
                                ${getStatusIcon(task.status)} ${task.status}
                            </span>
                        </span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">优先级：</span>
                        <span class="info-value">
                            <span class="priority-tag ${getPriorityClass(task.priority || '中')}">
                                ${getPriorityIcon(task.priority || '中')} ${task.priority || '中'}
                            </span>
                        </span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">生产日期：</span>
                        <span class="info-value">${task.productionDate}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">下单日期：</span>
                        <span class="info-value">${task.orderDate}</span>
                    </div>
                </div>
                
                <div class="info-card">
                    <h4>产品信息</h4>
                    <div class="info-item">
                        <span class="info-label">产品名称：</span>
                        <span class="info-value">${task.productName}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">产品编号：</span>
                        <span class="info-value">${task.productCode}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">生产批次：</span>
                        <span class="info-value">${task.batchNumber}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">计划数量：</span>
                        <span class="info-value">${task.plannedQty.toLocaleString()}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">已完成：</span>
                        <span class="info-value">${task.completedQty.toLocaleString()}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">包装比例：</span>
                        <span class="info-value">${task.packagingRatio}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">计量单位：</span>
                        <span class="info-value">${task.unit}</span>
                    </div>
                </div>
            </div>
        </div>
    `;
}

function createProgressContent(task) {
    const progress = task.progress || Math.round((task.completedQty / task.plannedQty) * 100);
    return `
        <div class="tab-pane active">
            <div class="progress-overview">
                <div class="progress-card">
                    <div class="card-title">总体进度</div>
                    <div class="card-value">${progress}%</div>
                    <div class="card-subtitle">已完成 ${task.completedQty.toLocaleString()} / ${task.plannedQty.toLocaleString()}</div>
                </div>
                <div class="progress-card">
                    <div class="card-title">预计剩余时间</div>
                    <div class="card-value">2.5小时</div>
                    <div class="card-subtitle">基于当前速度估算</div>
                </div>
                <div class="progress-card">
                    <div class="card-title">平均速度</div>
                    <div class="card-value">120/小时</div>
                    <div class="card-subtitle">最近1小时统计</div>
                </div>
            </div>
            <div class="info-card">
                <h4>详细统计</h4>
                <div class="info-item">
                    <span class="info-label">开始时间：</span>
                    <span class="info-value">${task.productionDate} 09:00:00</span>
                </div>
                <div class="info-item">
                    <span class="info-label">预计完成：</span>
                    <span class="info-value">${task.productionDate} 17:30:00</span>
                </div>
                <div class="info-item">
                    <span class="info-label">已用时间：</span>
                    <span class="info-value">5小时30分钟</span>
                </div>
                <div class="info-item">
                    <span class="info-label">成功率：</span>
                    <span class="info-value">98.5%</span>
                </div>
            </div>
        </div>
    `;
}

function createDetailsContent(task) {
    return `
        <div class="tab-pane active">
            <table class="details-table">
                <thead>
                    <tr>
                        <th>时间</th>
                        <th>操作类型</th>
                        <th>数量</th>
                        <th>状态</th>
                        <th>操作员</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>14:30:25</td>
                        <td>码采集</td>
                        <td>120</td>
                        <td><span class="status-tag completed">✅ 成功</span></td>
                        <td>操作员01</td>
                    </tr>
                    <tr>
                        <td>14:25:18</td>
                        <td>码采集</td>
                        <td>115</td>
                        <td><span class="status-tag completed">✅ 成功</span></td>
                        <td>操作员01</td>
                    </tr>
                    <tr>
                        <td>14:20:12</td>
                        <td>码采集</td>
                        <td>118</td>
                        <td><span class="status-tag completed">✅ 成功</span></td>
                        <td>操作员01</td>
                    </tr>
                </tbody>
            </table>
        </div>
    `;
}

function createExceptionsContent(task) {
    return `
        <div class="tab-pane active">
            <div class="log-list">
                <div class="log-item error">
                    <div class="log-time">2024-12-01 14:15:30</div>
                    <div class="log-content">扫码器连接异常，已自动重连</div>
                </div>
                <div class="log-item warning">
                    <div class="log-time">2024-12-01 13:45:12</div>
                    <div class="log-content">检测到重复码，已跳过处理</div>
                </div>
                <div class="log-item info">
                    <div class="log-time">2024-12-01 13:30:05</div>
                    <div class="log-content">系统自动校准完成</div>
                </div>
            </div>
        </div>
    `;
}

function createLogsContent(task) {
    return `
        <div class="tab-pane active">
            <div class="log-list">
                <div class="log-item info">
                    <div class="log-time">2024-12-01 14:30:25</div>
                    <div class="log-content">操作员01 执行码采集操作，成功处理120个</div>
                </div>
                <div class="log-item info">
                    <div class="log-time">2024-12-01 14:25:18</div>
                    <div class="log-content">操作员01 执行码采集操作，成功处理115个</div>
                </div>
                <div class="log-item info">
                    <div class="log-time">2024-12-01 09:00:00</div>
                    <div class="log-content">系统 创建任务 ${task.productionOrder}</div>
                </div>
            </div>
        </div>
    `;
}

// 任务操作函数
function editTask(taskId) {
    showMessage(`编辑任务 ${taskId}`, 'info');
    // 这里可以跳转到任务编辑页面
}

function startTask(taskId) {
    const task = mockTasks.find(t => t.id === taskId);
    if (task) {
        task.status = '进行中';
        showMessage(`任务 ${task.productionOrder} 已开始执行`, 'success');
        refreshTaskRow(taskId);
    }
}

function pauseTask(taskId) {
    const task = mockTasks.find(t => t.id === taskId);
    if (task) {
        task.status = '已暂停';
        showMessage(`任务 ${task.productionOrder} 已暂停`, 'warning');
        refreshTaskRow(taskId);
    }
}

function resumeTask(taskId) {
    const task = mockTasks.find(t => t.id === taskId);
    if (task) {
        task.status = '进行中';
        showMessage(`任务 ${task.productionOrder} 已继续执行`, 'success');
        refreshTaskRow(taskId);
    }
}

function stopTask(taskId) {
    if (confirm('确定要停止此任务吗？停止后需要重新开始。')) {
        const task = mockTasks.find(t => t.id === taskId);
        if (task) {
            task.status = '已停止';
            showMessage(`任务 ${task.productionOrder} 已停止`, 'warning');
            refreshTaskRow(taskId);
        }
    }
}

function restartTask(taskId) {
    const task = mockTasks.find(t => t.id === taskId);
    if (task) {
        task.status = '进行中';
        showMessage(`任务 ${task.productionOrder} 已重新开始`, 'success');
        refreshTaskRow(taskId);
    }
}

function removeTask(taskId) {
    if (confirm('确定要移除此任务吗？移除后无法恢复。')) {
        const index = mockTasks.findIndex(t => t.id === taskId);
        if (index !== -1) {
            const task = mockTasks[index];
            mockTasks.splice(index, 1);
            showMessage(`任务 ${task.productionOrder} 已移除`, 'success');
            loadTaskList();
        }
    }
}

// 任务管理弹框功能
function showTaskManagementModal() {
    const modal = document.getElementById('taskManagementModal');
    if (modal) {
        modal.style.display = 'flex';
    }
}

function closeTaskManagementModal() {
    const modal = document.getElementById('taskManagementModal');
    modal.style.display = 'none';
}

// 添加任务弹框功能
function showAddTaskModal() {
    const modal = document.getElementById('addTaskModal');
    if (modal) {
        modal.style.display = 'flex';
        
        // 设置默认日期
        const today = new Date().toISOString().split('T')[0];
        const productionDateInput = document.querySelector('input[name="productionDate"]');
        if (productionDateInput) {
            productionDateInput.value = today;
        }
    }
}

function closeAddTaskModal() {
    const modal = document.getElementById('addTaskModal');
    modal.style.display = 'none';
    
    // 重置表单
    const form = document.getElementById('addTaskForm');
    if (form) {
        form.reset();
    }
}

// 产品选择处理
function onProductSelect(productId) {
    const productData = {
        'P001': { code: 'P001', name: '维生素C片', ratio: '1:100', unit: '瓶' },
        'P002': { code: 'P002', name: '感冒灵颗粒', ratio: '1:50', unit: '盒' },
        'P003': { code: 'P003', name: '阿莫西林胶囊', ratio: '1:200', unit: '瓶' }
    };
    
    const product = productData[productId];
    if (product) {
        document.querySelector('input[name="productCode"]').value = product.code;
        document.querySelector('input[name="productName"]').value = product.name;
        document.querySelector('input[name="packagingRatio"]').value = product.ratio;
        document.querySelector('input[name="unit"]').value = product.unit;
    } else {
        // 清空字段
        document.querySelector('input[name="productCode"]').value = '';
        document.querySelector('input[name="productName"]').value = '';
        document.querySelector('input[name="packagingRatio"]').value = '';
        document.querySelector('input[name="unit"]').value = '';
    }
}

// 保存任务
function saveTask() {
    const form = document.getElementById('addTaskForm');
    const formData = new FormData(form);
    
    // 验证必填字段
    const requiredFields = ['productionOrder', 'productId', 'plannedQuantity', 'batchNumber', 'productionDate'];
    for (const field of requiredFields) {
        if (!formData.get(field)) {
            showMessage(`请填写${getFieldLabel(field)}`, 'warning');
            return;
        }
    }
    
    // 创建新任务
    const newTask = {
        id: mockTasks.length + 1,
        productionOrder: formData.get('productionOrder'),
        productCode: formData.get('productCode'),
        productName: formData.get('productName'),
        plannedQty: parseInt(formData.get('plannedQuantity')),
        completedQty: 0,
        packagingRatio: formData.get('packagingRatio'),
        unit: formData.get('unit'),
        batchNumber: formData.get('batchNumber'),
        productionDate: formData.get('productionDate'),
        expiryDate: formData.get('expiryDate') || '',
        orderDate: new Date().toISOString().split('T')[0],
        status: '待开始',
        remarks: formData.get('remarks') || ''
    };
    
    mockTasks.unshift(newTask);
    showMessage(`任务 ${newTask.productionOrder} 添加成功`, 'success');
    closeAddTaskModal();
    loadTaskList();
}

function getFieldLabel(field) {
    const labels = {
        'productionOrder': '生产订单',
        'productId': '产品',
        'plannedQuantity': '计划数量',
        'batchNumber': '生产批次',
        'productionDate': '生产日期'
    };
    return labels[field] || field;
}

// 自动同步功能
function syncTasks() {
    showMessage('正在同步任务数据...', 'info');
    
    // 模拟同步过程
    setTimeout(() => {
        // 随机添加1-3个新任务
        const newTaskCount = Math.floor(Math.random() * 3) + 1;
        const products = [
            { code: 'P001', name: '维生素C片', ratio: '1:100', unit: '瓶' },
            { code: 'P002', name: '感冒灵颗粒', ratio: '1:50', unit: '盒' },
            { code: 'P003', name: '阿莫西林胶囊', ratio: '1:200', unit: '瓶' }
        ];
        
        for (let i = 0; i < newTaskCount; i++) {
            const product = products[Math.floor(Math.random() * products.length)];
            const today = new Date();
            const newTask = {
                id: mockTasks.length + 1 + i,
                productionOrder: `PO${today.getFullYear()}${String(today.getMonth() + 1).padStart(2, '0')}${String(mockTasks.length + 1 + i).padStart(4, '0')}`,
                productCode: product.code,
                productName: product.name,
                plannedQty: Math.floor(Math.random() * 3000) + 1000,
                completedQty: 0,
                packagingRatio: product.ratio,
                unit: product.unit,
                batchNumber: `B${today.getFullYear()}${String(Math.floor(Math.random() * 1000)).padStart(3, '0')}`,
                productionDate: today.toISOString().split('T')[0],
                expiryDate: new Date(today.getTime() + 2 * 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
                orderDate: today.toISOString().split('T')[0],
                status: '待开始',
                remarks: `自动同步任务${i + 1}`
            };
            mockTasks.unshift(newTask);
        }
        
        showMessage(`同步完成，新增 ${newTaskCount} 个任务`, 'success');
        loadTaskList();
    }, 1500);
}

function refreshTaskRow(taskId) {
    const row = document.querySelector(`tr[data-task-id="${taskId}"]`);
    if (row) {
        row.classList.add('table-row-updated');
        setTimeout(() => {
            row.classList.remove('table-row-updated');
        }, 1000);
        
        // 重新渲染整个列表以更新状态
        renderTaskList();
    }
}

// 批量操作功能
function toggleBatchMenu(button) {
    const dropdown = button.parentElement.querySelector('.batch-dropdown');
    const isVisible = dropdown.classList.contains('show');
    
    // 关闭所有其他下拉菜单
    document.querySelectorAll('.batch-dropdown.show').forEach(menu => {
        menu.classList.remove('show');
    });
    
    // 切换当前菜单
    if (!isVisible) {
        dropdown.classList.add('show');
        
        // 点击其他地方关闭菜单
        setTimeout(() => {
            document.addEventListener('click', function closeMenu(e) {
                if (!button.parentElement.contains(e.target)) {
                    dropdown.classList.remove('show');
                    document.removeEventListener('click', closeMenu);
                }
            });
        }, 0);
    }
}

function handleBatchDelete() {
    if (selectedTasks.length === 0) {
        showMessage('请先选择要删除的任务', 'warning');
        return;
    }
    
    if (confirm(`确定要删除选中的 ${selectedTasks.length} 个任务吗？删除后无法恢复。`)) {
        selectedTasks.forEach(taskId => {
            const index = mockTasks.findIndex(t => t.id === taskId);
            if (index !== -1) {
                mockTasks.splice(index, 1);
            }
        });
        
        showMessage(`已删除 ${selectedTasks.length} 个任务`, 'success');
        selectedTasks = [];
        loadTaskList();
    }
}

function handleBatchExport() {
    if (selectedTasks.length === 0) {
        showMessage('请先选择要导出的任务', 'warning');
        return;
    }
    
    showMessage(`正在导出 ${selectedTasks.length} 个任务...`, 'info');
    // 这里实现导出逻辑
}

function handleBatchStatusChange() {
    if (selectedTasks.length === 0) {
        showMessage('请先选择要修改状态的任务', 'warning');
        return;
    }
    
    showMessage('批量状态修改功能开发中...', 'info');
}

function handleBatchPriorityChange() {
    if (selectedTasks.length === 0) {
        showMessage('请先选择要修改优先级的任务', 'warning');
        return;
    }
    
    showMessage('批量优先级设置功能开发中...', 'info');
}

// 工具栏操作
function handleNewTask() {
    window.location.href = 'task-create.html';
}

function handleRefresh() {
    showMessage('正在刷新任务列表...', 'info');
    loadTaskList();
}

function handleImportTasks() {
    showMessage('任务导入功能开发中...', 'info');
}

function handleExportTasks() {
    showMessage('任务导出功能开发中...', 'info');
}

// 高级筛选
function showAdvancedFilter() {
    const modal = document.getElementById('advancedFilterModal');
    modal.style.display = 'flex';
}

function closeAdvancedFilter() {
    const modal = document.getElementById('advancedFilterModal');
    modal.style.display = 'none';
}

function resetAdvancedFilter() {
    const form = document.getElementById('advancedFilterForm');
    form.reset();
}

function applyAdvancedFilter() {
    showMessage('高级筛选功能开发中...', 'info');
    closeAdvancedFilter();
}

// 自动刷新
function startAutoRefresh() {
    autoRefreshTimer = setInterval(() => {
        // 模拟数据更新
        updateMockData();
        if (document.visibilityState === 'visible') {
            loadTaskList();
        }
    }, 30000); // 30秒刷新一次
}

function stopAutoRefresh() {
    if (autoRefreshTimer) {
        clearInterval(autoRefreshTimer);
        autoRefreshTimer = null;
    }
}

function updateMockData() {
    // 模拟一些任务状态的变化
    mockTasks.forEach(task => {
        if (task.status === '进行中' && Math.random() < 0.1) {
            // 10% 概率更新进度
            const increment = Math.floor(Math.random() * 50) + 10;
            task.completedQty = Math.min(task.plannedQty, task.completedQty + increment);
            
            // 如果完成了，更新状态
            if (task.completedQty >= task.plannedQty) {
                task.status = '已完成';
            }
        }
    });
}

// 加载和错误处理
function showLoading() {
    const tableContainer = document.querySelector('.table-container');
    if (tableContainer && !tableContainer.querySelector('.loading-overlay')) {
        const loadingOverlay = document.createElement('div');
        loadingOverlay.className = 'loading-overlay';
        loadingOverlay.innerHTML = '<div class="loading-spinner"></div>';
        tableContainer.style.position = 'relative';
        tableContainer.appendChild(loadingOverlay);
    }
}

function hideLoading() {
    const loadingOverlay = document.querySelector('.loading-overlay');
    if (loadingOverlay) {
        loadingOverlay.remove();
    }
}

// 消息提示函数
function showMessage(text, type = 'info') {
    console.log(`[${type}] ${text}`);
    // 简化版本，只在控制台显示
}

// 强制加载数据函数
function forceLoadData() {
    console.log('Force loading data...');
    generateMockData();
    loadTaskList();
    showMessage('数据已强制重新加载', 'success');
}

// 全局函数
window.forceLoadData = forceLoadData;
window.handleNewTask = handleNewTask;
window.handleRefresh = handleRefresh;
window.handleImportTasks = handleImportTasks;
window.handleExportTasks = handleExportTasks;
window.toggleBatchMenu = toggleBatchMenu;
window.handleBatchDelete = handleBatchDelete;
window.handleBatchExport = handleBatchExport;
window.handleBatchStatusChange = handleBatchStatusChange;
window.handleBatchPriorityChange = handleBatchPriorityChange;
window.showAdvancedFilter = showAdvancedFilter;
window.closeAdvancedFilter = closeAdvancedFilter;
window.resetAdvancedFilter = resetAdvancedFilter;
window.applyAdvancedFilter = applyAdvancedFilter;
window.showTaskDetail = showTaskDetail;
window.closeTaskDetail = closeTaskDetail;
window.handleTaskSelect = handleTaskSelect;
window.goToPage = goToPage;
window.editTask = editTask;
window.startTask = startTask;
window.pauseTask = pauseTask;
window.resumeTask = resumeTask;
window.stopTask = stopTask;
window.restartTask = restartTask;
window.removeTask = removeTask;
window.showTaskManagementModal = showTaskManagementModal;
window.closeTaskManagementModal = closeTaskManagementModal;
window.showAddTaskModal = showAddTaskModal;
window.closeAddTaskModal = closeAddTaskModal;
window.onProductSelect = onProductSelect;
window.saveTask = saveTask;
window.syncTasks = syncTasks; 