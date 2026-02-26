// 任务选择页面JavaScript功能 - 工控机触摸屏优化

// 全局变量
let allTasks = [];
let filteredTasks = [];
let currentPage = 1;
let pageSize = 20;
let selectedTaskId = null;
let currentStatusFilter = '';
let searchTerm = '';
let advancedFilters = {};

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    initializePage();
});

function initializePage() {
    // 更新时间显示
    updateTime();
    setInterval(updateTime, 1000);
    
    // 初始化事件监听
    initEventListeners();
    
    // 生成模拟数据
    generateMockTasks();
    
    // 加载任务列表
    loadTaskList();
    
    // 显示欢迎消息
    setTimeout(() => {
        showMessage('任务选择界面加载完成', 'info');
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

function initEventListeners() {
    // 搜索输入框
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', debounce(handleSearch, 300));
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                performSearch();
            }
        });
    }
    
    // 高级筛选控件
    const priorityFilter = document.getElementById('priorityFilter');
    const createDateFilter = document.getElementById('createDateFilter');
    const productTypeFilter = document.getElementById('productTypeFilter');
    
    if (priorityFilter) {
        priorityFilter.addEventListener('change', updateAdvancedFilters);
    }
    if (createDateFilter) {
        createDateFilter.addEventListener('change', updateAdvancedFilters);
    }
    if (productTypeFilter) {
        productTypeFilter.addEventListener('change', updateAdvancedFilters);
    }
    
    // 分页大小选择
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', handlePageSizeChange);
    }
}

// 生成模拟任务数据
function generateMockTasks() {
    const statuses = ['待开始', '进行中', '已暂停'];
    const priorities = ['高', '中', '低'];
    const products = ['维生素C片', '感冒灵颗粒', '阿莫西林胶囊', '板蓝根颗粒', '复合维生素片'];
    const creators = ['张三', '李四', '王五', '赵六', '钱七'];
    
    allTasks = [];
    
    for (let i = 1; i <= 50; i++) {
        const createDate = new Date();
        createDate.setDate(createDate.getDate() - Math.floor(Math.random() * 7)); // 最近7天内创建
        
        const plannedQty = Math.floor(Math.random() * 5000) + 1000;
        const status = statuses[Math.floor(Math.random() * statuses.length)];
        
        const task = {
            id: i,
            taskNumber: `T${createDate.getFullYear()}${String(createDate.getMonth() + 1).padStart(2, '0')}${String(createDate.getDate()).padStart(2, '0')}${String(i).padStart(3, '0')}`,
            productName: products[Math.floor(Math.random() * products.length)],
            batchNumber: `B${createDate.getFullYear()}${String(Math.floor(Math.random() * 1000)).padStart(3, '0')}`,
            plannedQty: plannedQty,
            status: status,
            priority: priorities[Math.floor(Math.random() * priorities.length)],
            createTime: createDate.toISOString().split('T')[0] + ' ' + createDate.toTimeString().split(' ')[0].substring(0, 5),
            creator: creators[Math.floor(Math.random() * creators.length)],
            productType: products[Math.floor(Math.random() * products.length)],
            packageRatio: getRandomPackageRatio()
        };
        
        allTasks.push(task);
    }
}

function getRandomPackageRatio() {
    const ratios = [
        { level: 2, ratio: '1:100', description: '单品码 → 虚拟垛标' },
        { level: 3, ratio: '1:12:100', description: '瓶码 → 箱码 → 虚拟垛标' },
        { level: 4, ratio: '1:6:12:100', description: '瓶码 → 盒码 → 箱码 → 虚拟垛标' }
    ];
    return ratios[Math.floor(Math.random() * ratios.length)];
}

// 加载任务列表
function loadTaskList() {
    showLoading();
    
    // 模拟异步加载
    setTimeout(() => {
        applyFilters();
        renderTaskList();
        renderPagination();
        updateStatusCounts();
        updateFilterStatus();
        hideLoading();
    }, 300);
}

function applyFilters() {
    filteredTasks = allTasks.filter(task => {
        // 状态筛选
        if (currentStatusFilter && task.status !== currentStatusFilter) {
            return false;
        }
        
        // 搜索筛选
        if (searchTerm) {
            const searchFields = [
                task.taskNumber,
                task.productName,
                task.batchNumber
            ].join(' ').toLowerCase();
            
            if (!searchFields.includes(searchTerm.toLowerCase())) {
                return false;
            }
        }
        
        // 高级筛选
        if (advancedFilters.priority && task.priority !== advancedFilters.priority) {
            return false;
        }
        
        if (advancedFilters.createDate && !task.createTime.startsWith(advancedFilters.createDate)) {
            return false;
        }
        
        if (advancedFilters.productType && task.productName !== advancedFilters.productType) {
            return false;
        }
        
        return true;
    });
}

function renderTaskList() {
    const tbody = document.getElementById('taskTableBody');
    const emptyState = document.getElementById('emptyState');
    const tableContainer = document.querySelector('.task-table-container');
    
    if (!tbody) return;
    
    // 计算当前页数据
    const totalTasks = filteredTasks.length;
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = Math.min(startIndex + pageSize, totalTasks);
    const currentPageTasks = filteredTasks.slice(startIndex, endIndex);
    
    // 更新总数显示
    document.getElementById('totalTaskCount').textContent = totalTasks;
    document.getElementById('totalRecords').textContent = totalTasks;
    document.getElementById('currentPageNum').textContent = currentPage;
    document.getElementById('totalPages').textContent = Math.ceil(totalTasks / pageSize) || 1;
    
    // 清空表格
    tbody.innerHTML = '';
    
    if (currentPageTasks.length === 0) {
        tableContainer.style.display = 'none';
        emptyState.style.display = 'flex';
        
        if (searchTerm || currentStatusFilter || Object.keys(advancedFilters).length > 0) {
            emptyState.querySelector('.empty-text').textContent = '未找到符合条件的任务';
            emptyState.querySelector('.empty-action').style.display = 'none';
        } else {
            emptyState.querySelector('.empty-text').textContent = '暂无可执行的生产任务';
            emptyState.querySelector('.empty-action').style.display = 'block';
        }
        return;
    }
    
    tableContainer.style.display = 'block';
    emptyState.style.display = 'none';
    
    // 渲染数据行
    currentPageTasks.forEach(task => {
        const row = createTaskRow(task);
        tbody.appendChild(row);
    });
}

function createTaskRow(task) {
    const row = document.createElement('tr');
    row.dataset.taskId = task.id;
    
    // 如果是选中的任务，添加选中样式
    if (selectedTaskId === task.id) {
        row.classList.add('selected');
    }
    
    row.innerHTML = `
        <td>
            <input type="radio" name="selectedTask" class="task-radio" value="${task.id}" 
                   ${selectedTaskId === task.id ? 'checked' : ''} 
                   onchange="selectTask(${task.id})">
        </td>
        <td>
            <a href="javascript:void(0)" class="task-number-link" onclick="showTaskDetail(${task.id})">
                ${task.taskNumber}
            </a>
        </td>
        <td title="${task.productName}">${truncateText(task.productName, 12)}</td>
        <td>${task.plannedQty.toLocaleString()}</td>
        <td>
            <span class="status-tag ${getStatusClass(task.status)}">
                ${getStatusIcon(task.status)} ${task.status}
            </span>
        </td>
        <td>${task.createTime}</td>
        <td>
            <span class="priority-tag priority-${getPriorityClass(task.priority)}">
                ${getPriorityIcon(task.priority)} ${task.priority}
            </span>
        </td>
        <td>
            <button class="action-btn primary" onclick="selectTask(${task.id})" title="选择此任务">
                选择
            </button>
        </td>
    `;
    
    // 添加行点击事件
    row.addEventListener('click', function(e) {
        if (e.target.type !== 'radio' && !e.target.classList.contains('task-number-link') && !e.target.classList.contains('action-btn')) {
            selectTask(task.id);
        }
    });
    
    return row;
}

// 辅助函数
function truncateText(text, maxLength) {
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

function getStatusClass(status) {
    const statusMap = {
        '待开始': 'pending',
        '进行中': 'running',
        '已暂停': 'paused',
        '已完成': 'completed',
        '已停止': 'stopped'
    };
    return statusMap[status] || 'pending';
}

function getStatusIcon(status) {
    const iconMap = {
        '待开始': '⏳',
        '进行中': '🔄',
        '已暂停': '⏸️',
        '已完成': '✅',
        '已停止': '⏹️'
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

// 状态筛选
function filterByStatus(status) {
    currentStatusFilter = status;
    currentPage = 1;
    
    // 更新标签页状态
    document.querySelectorAll('.status-tab').forEach(tab => {
        tab.classList.remove('active');
    });
    
    const activeTab = document.querySelector(`[data-status="${status}"]`);
    if (activeTab) {
        activeTab.classList.add('active');
    }
    
    loadTaskList();
}

// 搜索功能
function handleSearch() {
    searchTerm = document.getElementById('searchInput').value.trim();
    currentPage = 1;
    loadTaskList();
}

function performSearch() {
    handleSearch();
}

function clearSearch() {
    document.getElementById('searchInput').value = '';
    searchTerm = '';
    currentPage = 1;
    loadTaskList();
}

// 高级筛选
function toggleAdvancedFilter() {
    const panel = document.getElementById('advancedFilterPanel');
    const btn = document.querySelector('.advanced-filter-btn');
    
    if (panel.style.display === 'none' || !panel.style.display) {
        panel.style.display = 'block';
        btn.classList.add('expanded');
    } else {
        panel.style.display = 'none';
        btn.classList.remove('expanded');
    }
}

function updateAdvancedFilters() {
    advancedFilters = {
        priority: document.getElementById('priorityFilter').value,
        createDate: document.getElementById('createDateFilter').value,
        productType: document.getElementById('productTypeFilter').value
    };
    
    // 移除空值
    Object.keys(advancedFilters).forEach(key => {
        if (!advancedFilters[key]) {
            delete advancedFilters[key];
        }
    });
    
    currentPage = 1;
    loadTaskList();
}

function resetFilters() {
    document.getElementById('priorityFilter').value = '';
    document.getElementById('createDateFilter').value = '';
    document.getElementById('productTypeFilter').value = '';
    advancedFilters = {};
    currentPage = 1;
    loadTaskList();
}

function applyFilters() {
    updateAdvancedFilters();
}

// 任务选择
function selectTask(taskId) {
    selectedTaskId = taskId;
    
    // 更新单选框状态
    document.querySelectorAll('.task-radio').forEach(radio => {
        radio.checked = (parseInt(radio.value) === taskId);
    });
    
    // 更新行选中状态
    document.querySelectorAll('.task-table tbody tr').forEach(row => {
        row.classList.remove('selected');
        if (parseInt(row.dataset.taskId) === taskId) {
            row.classList.add('selected');
        }
    });
    
    // 更新确认按钮状态
    const confirmBtn = document.getElementById('confirmBtn');
    const selectedCount = document.getElementById('selectedCount');
    
    if (taskId) {
        confirmBtn.disabled = false;
        selectedCount.textContent = '1';
    } else {
        confirmBtn.disabled = true;
        selectedCount.textContent = '0';
    }
    
    // 提供触觉反馈
    if (navigator.vibrate) {
        navigator.vibrate(50);
    }
}

// 任务详情
function showTaskDetail(taskId) {
    const task = allTasks.find(t => t.id === taskId);
    if (!task) return;
    
    const modal = document.getElementById('taskDetailModal');
    const title = document.getElementById('taskDetailTitle');
    const content = document.getElementById('taskDetailContent');
    const selectBtn = document.getElementById('selectTaskBtn');
    
    title.textContent = `任务详情 - ${task.taskNumber}`;
    content.innerHTML = createTaskDetailContent(task);
    
    // 设置选择按钮状态
    if (selectedTaskId === taskId) {
        selectBtn.textContent = '已选择此任务';
        selectBtn.disabled = true;
    } else {
        selectBtn.textContent = '选择此任务';
        selectBtn.disabled = false;
        selectBtn.onclick = () => selectTaskFromDetail(taskId);
    }
    
    modal.style.display = 'flex';
}

function createTaskDetailContent(task) {
    return `
        <div class="info-grid">
            <div class="info-card">
                <h4>任务信息</h4>
                <div class="info-item">
                    <span class="info-label">任务单号：</span>
                    <span class="info-value">${task.taskNumber}</span>
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
                        <span class="priority-tag priority-${getPriorityClass(task.priority)}">
                            ${getPriorityIcon(task.priority)} ${task.priority}
                        </span>
                    </span>
                </div>
                <div class="info-item">
                    <span class="info-label">创建时间：</span>
                    <span class="info-value">${task.createTime}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">创建人：</span>
                    <span class="info-value">${task.creator}</span>
                </div>
            </div>
            
            <div class="info-card">
                <h4>产品信息</h4>
                <div class="info-item">
                    <span class="info-label">产品名称：</span>
                    <span class="info-value">${task.productName}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">生产批号：</span>
                    <span class="info-value">${task.batchNumber}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">计划数量：</span>
                    <span class="info-value">${task.plannedQty.toLocaleString()}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">包装比例：</span>
                    <span class="info-value">${task.packageRatio.ratio}</span>
                </div>
                <div class="info-item">
                    <span class="info-label">采集模式：</span>
                    <span class="info-value">${task.packageRatio.level}级关联</span>
                </div>
            </div>
        </div>
        
        <div class="info-card">
            <h4>包装配置详情</h4>
            <div class="info-item">
                <span class="info-label">关联层级：</span>
                <span class="info-value">${task.packageRatio.level}级关联</span>
            </div>
            <div class="info-item">
                <span class="info-label">包装描述：</span>
                <span class="info-value">${task.packageRatio.description}</span>
            </div>
            <div class="info-item">
                <span class="info-label">自动适配：</span>
                <span class="info-value">已启用</span>
            </div>
        </div>
    `;
}

function selectTaskFromDetail(taskId) {
    if (taskId) {
        selectTask(taskId);
        closeTaskDetail();
        showMessage('任务已选择', 'success');
    }
}

function closeTaskDetail() {
    const modal = document.getElementById('taskDetailModal');
    modal.style.display = 'none';
}

// 确认选择
function confirmSelection() {
    if (!selectedTaskId) {
        showMessage('请先选择一个任务', 'warning');
        return;
    }
    
    const task = allTasks.find(t => t.id === selectedTaskId);
    if (!task) {
        showMessage('选择的任务不存在', 'error');
        return;
    }
    
    // 显示确认对话框
    showConfirmModal(task);
}

function showConfirmModal(task) {
    const modal = document.getElementById('confirmModal');
    const taskInfo = document.getElementById('selectedTaskInfo');
    
    taskInfo.innerHTML = `
        <div><strong>任务单号：</strong>${task.taskNumber}</div>
        <div><strong>产品名称：</strong>${task.productName}</div>
        <div><strong>计划数量：</strong>${task.plannedQty.toLocaleString()}</div>
        <div><strong>任务状态：</strong><span class="status-tag ${getStatusClass(task.status)}">${task.status}</span></div>
        <div><strong>优先级：</strong><span class="priority-tag priority-${getPriorityClass(task.priority)}">${getPriorityIcon(task.priority)} ${task.priority}</span></div>
    `;
    
    modal.style.display = 'flex';
}

function closeConfirmModal() {
    const modal = document.getElementById('confirmModal');
    modal.style.display = 'none';
}

function executeSelectedTask() {
    const task = allTasks.find(t => t.id === selectedTaskId);
    if (!task) return;
    
    closeConfirmModal();
    
    // 模拟跳转到任务执行控制界面
    showMessage(`正在启动任务 ${task.taskNumber}...`, 'info');
    
    setTimeout(() => {
        showMessage('任务启动成功，即将跳转到执行控制界面', 'success');
        setTimeout(() => {
            // 这里应该跳转到任务执行控制界面
            window.location.href = 'task-control.html?taskId=' + selectedTaskId;
        }, 1500);
    }, 1000);
}

function cancelSelection() {
    selectedTaskId = null;
    
    // 清除所有选择状态
    document.querySelectorAll('.task-radio').forEach(radio => {
        radio.checked = false;
    });
    
    document.querySelectorAll('.task-table tbody tr').forEach(row => {
        row.classList.remove('selected');
    });
    
    // 更新确认按钮状态
    const confirmBtn = document.getElementById('confirmBtn');
    const selectedCount = document.getElementById('selectedCount');
    
    confirmBtn.disabled = true;
    selectedCount.textContent = '0';
    
    showMessage('已取消选择', 'info');
}

// 分页功能
function renderPagination() {
    const paginationControls = document.getElementById('paginationControls');
    if (!paginationControls) return;
    
    const totalPages = Math.ceil(filteredTasks.length / pageSize);
    
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

// 状态统计
function updateStatusCounts() {
    const counts = {
        all: allTasks.length,
        pending: allTasks.filter(t => t.status === '待开始').length,
        running: allTasks.filter(t => t.status === '进行中').length,
        paused: allTasks.filter(t => t.status === '已暂停').length
    };
    
    document.getElementById('allCount').textContent = counts.all;
    document.getElementById('pendingCount').textContent = counts.pending;
    document.getElementById('runningCount').textContent = counts.running;
    document.getElementById('pausedCount').textContent = counts.paused;
}

function updateFilterStatus() {
    const filterStatus = document.getElementById('filterStatus');
    let statusText = '显示全部任务';
    
    if (currentStatusFilter) {
        statusText = `显示${currentStatusFilter}任务`;
    }
    
    if (searchTerm) {
        statusText += ` (搜索: ${searchTerm})`;
    }
    
    if (Object.keys(advancedFilters).length > 0) {
        statusText += ' (已应用高级筛选)';
    }
    
    filterStatus.textContent = statusText;
}

// 刷新功能
function refreshTaskList() {
    showMessage('正在刷新任务列表...', 'info');
    
    // 重新生成数据（模拟数据更新）
    generateMockTasks();
    
    // 重新加载列表
    loadTaskList();
    
    showMessage('任务列表已刷新', 'success');
}

// 帮助功能
function showHelp() {
    const modal = document.getElementById('helpModal');
    modal.style.display = 'flex';
}

function closeHelp() {
    const modal = document.getElementById('helpModal');
    modal.style.display = 'none';
}

// 高级筛选（从工具栏调用）
function showAdvancedFilter() {
    const panel = document.getElementById('advancedFilterPanel');
    const btn = document.querySelector('.advanced-filter-btn');
    
    panel.style.display = 'block';
    btn.classList.add('expanded');
}

// 加载和错误处理
function showLoading() {
    const loadingState = document.getElementById('loadingState');
    const tableContainer = document.querySelector('.task-table-container');
    
    if (loadingState && tableContainer) {
        loadingState.style.display = 'flex';
        tableContainer.style.display = 'none';
    }
}

function hideLoading() {
    const loadingState = document.getElementById('loadingState');
    
    if (loadingState) {
        loadingState.style.display = 'none';
    }
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

// 消息提示函数
function showMessage(text, type = 'info') {
    if (window.Message) {
        Message.show(text, type);
    } else {
        alert(text);
    }
}

// 全局函数
window.filterByStatus = filterByStatus;
window.performSearch = performSearch;
window.clearSearch = clearSearch;
window.toggleAdvancedFilter = toggleAdvancedFilter;
window.resetFilters = resetFilters;
window.applyFilters = applyFilters;
window.selectTask = selectTask;
window.showTaskDetail = showTaskDetail;
window.closeTaskDetail = closeTaskDetail;
window.selectTaskFromDetail = selectTaskFromDetail;
window.confirmSelection = confirmSelection;
window.closeConfirmModal = closeConfirmModal;
window.executeSelectedTask = executeSelectedTask;
window.cancelSelection = cancelSelection;
window.goToPage = goToPage;
window.refreshTaskList = refreshTaskList;
window.showHelp = showHelp;
window.closeHelp = closeHelp;
window.showAdvancedFilter = showAdvancedFilter; 