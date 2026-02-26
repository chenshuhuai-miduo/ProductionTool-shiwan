// 任务列表管理页面JavaScript功能

console.log('=== task-list-clean.js 开始加载 ===');

// 全局变量 - 使用taskList前缀避免与其他页面冲突，并确保全局可访问
window.taskListCurrentPage = 1;
window.taskListPageSize = 20;
window.taskListTotalTasks = 0;
window.taskListFilteredTasks = [];
window.taskListSelectedTasks = [];
window.taskListAutoRefreshTimer = null;
window.taskListMockTasks = [];

console.log('全局变量已初始化');

// 页面初始化 - 延迟执行以避免与desktop-app.js冲突
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, initializing task list page...');
    // 延迟执行以确保desktop-app.js先执行
    setTimeout(() => {
        console.log('开始初始化页面...');
        initializePage();
    }, 100);
});

// 备用初始化 - 如果DOMContentLoaded没有正常工作
window.addEventListener('load', function() {
    console.log('Window loaded, backup initialization...');
    if (window.taskListMockTasks.length === 0) {
        console.log('检测到数据未加载，执行备用初始化...');
        setTimeout(() => {
            initializePage();
        }, 200);
    }
});

// 立即执行测试
console.log('task-list-clean.js loaded successfully');

// 强制初始化函数 - 用于调试
window.forceInit = function() {
    console.log('强制初始化被调用');
    initializePage();
};

function initializePage() {
    console.log('=== 开始初始化页面 ===');
    
    try {
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
        console.log('Generated tasks:', window.taskListMockTasks.length);
        
        console.log('Step 4: Loading task list...');
        // 加载任务列表
        loadTaskList();
        
        console.log('Step 5: Starting auto refresh...');
        // 启动自动刷新
        startAutoRefresh();
        
        console.log('=== 初始化完成! ===');
    } catch (error) {
        console.error('初始化过程中发生错误:', error);
        console.error('错误堆栈:', error.stack);
    }
}

function initEventListeners() {
    // 搜索和筛选
    const productionOrderSearch = document.getElementById('productionOrderSearch');
    const batchNumberSearch = document.getElementById('batchNumberSearch');
    const productNameSearch = document.getElementById('productNameSearch');
    
    if (productionOrderSearch) {
        productionOrderSearch.addEventListener('input', debounce(handleSearch, 300));
        productionOrderSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                handleSearch();
            }
        });
    }
    
    if (batchNumberSearch) {
        batchNumberSearch.addEventListener('input', debounce(handleSearch, 300));
        batchNumberSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                handleSearch();
            }
        });
    }
    
    if (productNameSearch) {
        productNameSearch.addEventListener('input', debounce(handleSearch, 300));
        productNameSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                handleSearch();
            }
        });
    }
    
    // 分页大小选择
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', handlePageSizeChange);
    }
}

// 生成模拟数据
function generateMockData() {
    console.log('=== 开始生成模拟数据 ===');
    
    try {
        // 生成5个固定的示例数据
        const sampleTasks = [
            {
                id: 1,
                productionOrder: 'PO202412010001',
                productCode: 'P001',
                productName: '维生素C片',
                productSpec: '100mg/片',
                plannedQty: 5000,
                completedQty: 3200,
                packagingRatio: '1:100',
                batchNumber: 'B2024001',
                productionDate: '2024-12-01',
                expectedCompletionTime: '2024-12-03 18:00',
                orderDate: '2024-11-28',
                status: '生产中',
                remarks: '优先生产订单',
                progress: Math.round((3200 / 5000) * 100)
            },
            {
                id: 2,
                productionOrder: 'PO202412010002',
                productCode: 'P002',
                productName: '感冒灵颗粒',
                productSpec: '10g/袋',
                plannedQty: 3000,
                completedQty: 2800,
                packagingRatio: '1:50',
                batchNumber: 'B2024002',
                productionDate: '2024-12-01',
                expectedCompletionTime: '2024-12-02 16:30',
                orderDate: '2024-11-29',
                status: '生产中',
                remarks: '接近完成',
                progress: Math.round((2800 / 3000) * 100)
            },
            {
                id: 3,
                productionOrder: 'PO202412010003',
                productCode: 'P003',
                productName: '阿莫西林胶囊',
                productSpec: '250mg/粒',
                plannedQty: 8000,
                completedQty: 0,
                packagingRatio: '1:200',
                batchNumber: 'B2024003',
                productionDate: '2024-12-02',
                expectedCompletionTime: '2024-12-05 14:00',
                orderDate: '2024-11-30',
                status: '计划',
                remarks: '等待原料到货',
                progress: 0
            },
            {
                id: 4,
                productionOrder: 'PO202412010004',
                productCode: 'P004',
                productName: '板蓝根颗粒',
                productSpec: '15g/袋',
                plannedQty: 2500,
                completedQty: 1200,
                packagingRatio: '',
                batchNumber: 'B2024004',
                productionDate: '2024-12-01',
                expectedCompletionTime: '2024-12-04 10:00',
                orderDate: '2024-11-27',
                status: '生产中',
                remarks: '设备维护中',
                progress: Math.round((1200 / 2500) * 100)
            },
            {
                id: 5,
                productionOrder: 'PO202412010005',
                productCode: 'P005',
                productName: '复合维生素片',
                productSpec: '多维/片',
                plannedQty: 4500,
                completedQty: 800,
                packagingRatio: '1:120',
                batchNumber: 'B2024005',
                productionDate: '2024-12-01',
                expectedCompletionTime: '2024-12-06 12:00',
                orderDate: '2024-11-26',
                status: '生产完成',
                remarks: '包装材料不足',
                progress: Math.round((800 / 4500) * 100)
            }
        ];
        
        window.taskListMockTasks = [...sampleTasks];
        console.log('Mock data generated successfully:', window.taskListMockTasks.length, '条任务');
        console.log('任务详情:', window.taskListMockTasks);
    } catch (error) {
        console.error('生成模拟数据时发生错误:', error);
        window.taskListMockTasks = [];
    }
}

// 加载任务列表
function loadTaskList() {
    console.log('=== 开始加载任务列表 ===');
    
    try {
        console.log('taskListMockTasks before filtering:', window.taskListMockTasks.length);
        
        // 直接应用过滤器，不使用异步
        applyFilters();
        console.log('Filtered tasks after filtering:', window.taskListFilteredTasks.length);
        
        console.log('开始渲染任务列表...');
        renderTaskList();
        
        console.log('开始渲染分页...');
        renderPagination();
        
        console.log('=== 任务列表加载完成! ===');
    } catch (error) {
        console.error('加载任务列表时发生错误:', error);
        console.error('错误堆栈:', error.stack);
    }
}

function applyFilters() {
    console.log('=== 开始应用过滤器 ===');
    console.log('taskListMockTasks:', window.taskListMockTasks.length);
    
    try {
        const productionOrderSearch = document.getElementById('productionOrderSearch')?.value.toLowerCase() || '';
        const batchNumberSearch = document.getElementById('batchNumberSearch')?.value.toLowerCase() || '';
        const productNameSearch = document.getElementById('productNameSearch')?.value.toLowerCase() || '';
        
        console.log('Filter values:', { productionOrderSearch, batchNumberSearch, productNameSearch });
        
        window.taskListFilteredTasks = window.taskListMockTasks.filter(task => {
            // 不排除任何状态的任务，显示所有任务
            
            // 生产订单搜索
            if (productionOrderSearch && !task.productionOrder.toLowerCase().includes(productionOrderSearch)) {
                console.log(`生产订单不匹配: ${task.productionOrder}`);
                return false;
            }
            
            // 生产批次搜索
            if (batchNumberSearch && !task.batchNumber.toLowerCase().includes(batchNumberSearch)) {
                console.log(`生产批次不匹配: ${task.batchNumber}`);
                return false;
            }
            
            // 产品名称搜索
            if (productNameSearch && !task.productName.toLowerCase().includes(productNameSearch)) {
                console.log(`产品名称不匹配: ${task.productName}`);
                return false;
            }
            
            return true;
        });
        
        window.taskListTotalTasks = window.taskListFilteredTasks.length;
        console.log('过滤结果: taskListTotalTasks =', window.taskListTotalTasks);
        console.log('过滤后的任务:', window.taskListFilteredTasks);
    } catch (error) {
        console.error('应用过滤器时发生错误:', error);
        window.taskListFilteredTasks = [];
        window.taskListTotalTasks = 0;
    }
}

function renderTaskList() {
    console.log('=== 开始渲染任务列表 ===');
    console.log('taskListMockTasks:', window.taskListMockTasks.length);
    console.log('taskListFilteredTasks:', window.taskListFilteredTasks.length);
    console.log('taskListTotalTasks:', window.taskListTotalTasks);
    
    try {
        const tbody = document.getElementById('taskTableBody');
        const totalCountSpan = document.getElementById('totalCount');
        
        if (!tbody) {
            console.error('tbody element not found!');
            return;
        }
        
        console.log('找到tbody元素:', tbody);
        
        // 更新总数显示
        if (totalCountSpan) {
            totalCountSpan.textContent = window.taskListTotalTasks;
            console.log('更新总数显示:', window.taskListTotalTasks);
        } else {
            console.warn('totalCount element not found');
        }
        
        // 计算当前页数据
        const startIndex = (window.taskListCurrentPage - 1) * window.taskListPageSize;
        const endIndex = Math.min(startIndex + window.taskListPageSize, window.taskListTotalTasks);
        const currentPageTasks = window.taskListFilteredTasks.slice(startIndex, endIndex);
        
        console.log(`当前页数据: ${startIndex}-${endIndex}, 共${currentPageTasks.length}条`);
        
        // 清空表格
        tbody.innerHTML = '';
        
        if (currentPageTasks.length === 0) {
            console.log('No tasks to display, showing empty state');
            tbody.innerHTML = `
                <tr>
                    <td colspan="14" class="empty-state">
                        <div class="empty-icon">📋</div>
                        <div class="empty-text">暂无任务数据</div>
                        <div class="empty-action">
                            <button class="btn btn-primary" onclick="showAddTaskModal()">手动添加</button>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }
        
        console.log('Rendering', currentPageTasks.length, 'tasks');
        
        // 渲染数据行
        currentPageTasks.forEach((task, index) => {
            console.log(`渲染任务 ${index + 1}:`, task.productionOrder);
            const row = createTaskRow(task);
            tbody.appendChild(row);
        });
        
        console.log('=== 任务列表渲染完成 ===');
    } catch (error) {
        console.error('渲染任务列表时发生错误:', error);
        console.error('错误堆栈:', error.stack);
    }
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
            <a href="javascript:void(0)" onclick="showTaskDetail(${task.id})" class="task-link">
                ${task.productionOrder}
            </a>
        </td>
        <td>${task.productCode}</td>
        <td>${task.productName}</td>
        <td>${task.productSpec}</td>
        <td>${task.plannedQty.toLocaleString()}</td>
        <td>${task.completedQty.toLocaleString()}</td>
        <td>${task.packagingRatio}</td>
        <td>${task.batchNumber}</td>
        <td>${task.productionDate}</td>
        <td>${task.expectedCompletionTime}</td>
        <td>${task.orderDate}</td>
        <td>
            <span class="status-tag ${getStatusClass(task.status)}">
                ${task.status}
            </span>
        </td>
        <td>${task.remarks || '-'}</td>
        <td>
            <div class="action-buttons">
                <button class="action-btn edit" title="修改任务" onclick="editTask(${task.id})">✏️</button>
                <button class="action-btn delete" title="移除任务" onclick="removeTask(${task.id})">🗑️</button>
            </div>
        </td>
    `;
    
    return row;
}

// 辅助函数
function getStatusClass(status) {
    const statusMap = {
        '计划': 'pending',
        '生产中': 'running',
        '生产完成': 'completed'
    };
    return statusMap[status] || 'pending';
}



// 分页功能
function renderPagination() {
    const paginationControls = document.getElementById('paginationControls');
    if (!paginationControls) return;
    
    const totalPages = Math.ceil(window.taskListTotalTasks / window.taskListPageSize);
    
    if (totalPages <= 1) {
        paginationControls.innerHTML = '';
        return;
    }
    
    let paginationHTML = '';
    
    // 首页按钮
    paginationHTML += `<button ${window.taskListCurrentPage === 1 ? 'disabled' : ''} onclick="goToPage(1)">首页</button>`;
    
    // 上一页按钮
    paginationHTML += `<button ${window.taskListCurrentPage === 1 ? 'disabled' : ''} onclick="goToPage(${window.taskListCurrentPage - 1})">上一页</button>`;
    
    // 页码按钮
    const startPage = Math.max(1, window.taskListCurrentPage - 2);
    const endPage = Math.min(totalPages, window.taskListCurrentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        paginationHTML += `<button class="${i === window.taskListCurrentPage ? 'active' : ''}" onclick="goToPage(${i})">${i}</button>`;
    }
    
    // 下一页按钮
    paginationHTML += `<button ${window.taskListCurrentPage === totalPages ? 'disabled' : ''} onclick="goToPage(${window.taskListCurrentPage + 1})">下一页</button>`;
    
    // 末页按钮
    paginationHTML += `<button ${window.taskListCurrentPage === totalPages ? 'disabled' : ''} onclick="goToPage(${totalPages})">末页</button>`;
    
    paginationControls.innerHTML = paginationHTML;
}

function goToPage(page) {
    window.taskListCurrentPage = page;
    renderTaskList();
    renderPagination();
}

function handlePageSizeChange() {
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    window.taskListPageSize = parseInt(pageSizeSelect.value);
    window.taskListCurrentPage = 1;
    renderTaskList();
    renderPagination();
}

// 搜索和筛选
function handleSearch() {
    window.taskListCurrentPage = 1;
    loadTaskList();
}

function handleFilter() {
    window.taskListCurrentPage = 1;
    loadTaskList();
}

// 搜索任务函数
function searchTasks() {
    console.log('执行搜索...');
    handleSearch();
}

// 清空筛选条件
function clearFilters() {
    console.log('清空筛选条件...');
    
    // 清空所有搜索输入框
    const productionOrderSearch = document.getElementById('productionOrderSearch');
    const batchNumberSearch = document.getElementById('batchNumberSearch');
    const productNameSearch = document.getElementById('productNameSearch');
    
    if (productionOrderSearch) productionOrderSearch.value = '';
    if (batchNumberSearch) batchNumberSearch.value = '';
    if (productNameSearch) productNameSearch.value = '';
    
    // 重新加载任务列表
    handleSearch();
    
    showMessage('筛选条件已清空', 'info');
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

// 任务操作功能
function showTaskDetail(taskId) {
    const task = window.taskListMockTasks.find(t => t.id === taskId);
    if (!task) return;
    
    console.log('Show task detail for:', task.productionOrder);
    showMessage('任务详情功能开发中...', 'info');
}

function editTask(taskId) {
    showMessage(`编辑任务 ${taskId}`, 'info');
}

function removeTask(taskId) {
    if (confirm('确定要移除此任务吗？移除后无法恢复。')) {
        const index = window.taskListMockTasks.findIndex(t => t.id === taskId);
        if (index !== -1) {
            const task = window.taskListMockTasks[index];
            window.taskListMockTasks.splice(index, 1);
            showMessage(`任务 ${task.productionOrder} 已移除`, 'success');
            loadTaskList();
        }
    }
}

// 添加任务弹框功能
function showAddTaskModal() {
    console.log('=== showAddTaskModal 被调用 ===');
    
    try {
        const modal = document.getElementById('addTaskModal');
        console.log('查找模态框元素:', modal);
        
        if (modal) {
            console.log('找到模态框，开始初始化...');
            
            // 重置表单
            resetAddTaskForm();
            console.log('表单已重置');
            
            // 自动生成生产订单号
            generateProductionOrderNumber();
            console.log('生产订单号已生成');
            
            // 设置默认生产日期为今天
            setDefaultProductionDate();
            console.log('默认日期已设置');
            
            // 显示模态框
            modal.style.display = 'block';
            console.log('模态框已显示');
            
            // 添加点击外部关闭功能
            modal.onclick = function(e) {
                if (e.target === modal) {
                    console.log('点击外部关闭模态框');
                    closeAddTaskModal();
                }
            };
            
            console.log('=== showAddTaskModal 执行完成 ===');
        } else {
            console.error('未找到addTaskModal元素!');
        }
    } catch (error) {
        console.error('showAddTaskModal执行时发生错误:', error);
        console.error('错误堆栈:', error.stack);
    }
}

function closeAddTaskModal() {
    const modal = document.getElementById('addTaskModal');
    if (modal) {
        modal.style.display = 'none';
        resetAddTaskForm();
    }
}

function resetAddTaskForm() {
    const form = document.getElementById('addTaskForm');
    if (form) {
        form.reset();
        // 清空只读字段
        const readonlyFields = ['productCode', 'productName', 'productSpec', 'packagingRatio'];
        readonlyFields.forEach(fieldName => {
            const field = form.querySelector(`[name="${fieldName}"]`);
            if (field) {
                field.value = '';
            }
        });
    }
}

function generateProductionOrderNumber() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const random = String(Math.floor(Math.random() * 9999) + 1).padStart(4, '0');
    
    const orderNumber = `PO${year}${month}${day}${random}`;
    
    const orderField = document.querySelector('#addTaskForm [name="productionOrder"]');
    if (orderField) {
        orderField.value = orderNumber;
    }
}

function setDefaultProductionDate() {
    const now = new Date();
    const today = now.toISOString().split('T')[0];
    
    const dateField = document.querySelector('#addTaskForm [name="productionDate"]');
    if (dateField) {
        dateField.value = today;
    }
}

// 产品信息数据 - 使用独特的变量名避免冲突
const taskProductData = {
    'P001': {
        code: 'P001',
        name: '维生素C片',
        spec: '100mg/片',
        packagingRatio: '1:100'
    },
    'P002': {
        code: 'P002',
        name: '感冒灵颗粒',
        spec: '10g/袋',
        packagingRatio: '1:50'
    },
    'P003': {
        code: 'P003',
        name: '阿莫西林胶囊',
        spec: '250mg/粒',
        packagingRatio: '1:200'
    },
    'P004': {
        code: 'P004',
        name: '板蓝根颗粒',
        spec: '15g/袋',
        packagingRatio: ''
    },
    'P005': {
        code: 'P005',
        name: '复合维生素片',
        spec: '多维/片',
        packagingRatio: '1:120'
    },
    'P006': {
        code: 'P006',
        name: '布洛芬缓释胶囊',
        spec: '300mg/粒',
        packagingRatio: '1:60'
    },
    'P007': {
        code: 'P007',
        name: '头孢克肟颗粒',
        spec: '50mg/袋',
        packagingRatio: '1:40'
    },
    'P008': {
        code: 'P008',
        name: '蒙脱石散',
        spec: '3g/袋',
        packagingRatio: '1:30'
    }
};

function onProductSelect(productId) {
    const form = document.getElementById('addTaskForm');
    if (!form || !productId) {
        // 清空字段
        const fields = ['productCode', 'productName', 'productSpec', 'packagingRatio'];
        fields.forEach(fieldName => {
            const field = form.querySelector(`[name="${fieldName}"]`);
            if (field) field.value = '';
        });
        return;
    }
    
    const product = taskProductData[productId];
    if (product) {
        // 填充产品信息
        const codeField = form.querySelector('[name="productCode"]');
        const nameField = form.querySelector('[name="productName"]');
        const specField = form.querySelector('[name="productSpec"]');
        const ratioField = form.querySelector('[name="packagingRatio"]');
        
        if (codeField) codeField.value = product.code;
        if (nameField) nameField.value = product.name;
        if (specField) specField.value = product.spec;
        if (ratioField) ratioField.value = product.packagingRatio;
    }
}

function saveTask() {
    const form = document.getElementById('addTaskForm');
    if (!form) return;
    
    // 验证表单
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    // 收集表单数据
    const formData = new FormData(form);
    const taskData = {
        id: window.taskListMockTasks.length + 1,
        productionOrder: formData.get('productionOrder'),
        productCode: formData.get('productCode'),
        productName: formData.get('productName'),
        productSpec: formData.get('productSpec') || '',
        plannedQty: parseInt(formData.get('plannedQuantity')),
        completedQty: 0,
        packagingRatio: formData.get('packagingRatio'),
        batchNumber: formData.get('batchNumber'),
        productionDate: formData.get('productionDate'),
        expectedCompletionTime: formData.get('expectedCompletionTime') || '',
        orderDate: new Date().toISOString().split('T')[0],
        status: '计划',
        remarks: formData.get('remarks') || '',
        progress: 0
    };
    
    // 添加到任务列表
    window.taskListMockTasks.push(taskData);
    
    // 关闭模态框
    closeAddTaskModal();
    
    // 刷新列表
    loadTaskList();
    
    // 显示成功消息
    showMessage('任务添加成功！', 'success');
}

// 自动同步功能
function syncTasks() {
    showMessage('正在同步任务数据...', 'info');
    
    // 模拟同步过程
    setTimeout(() => {
        showMessage('同步完成', 'success');
        loadTaskList();
    }, 1500);
}

// 自动刷新
function startAutoRefresh() {
    window.taskListAutoRefreshTimer = setInterval(() => {
        if (document.visibilityState === 'visible') {
            // 模拟数据更新
            console.log('Auto refresh triggered');
        }
    }, 30000); // 30秒刷新一次
}

// 强制加载数据函数
function forceLoadData() {
    console.log('Force loading data...');
    generateMockData();
    loadTaskList();
    showMessage('数据已强制重新加载', 'success');
}

// 消息提示函数
function showMessage(text, type = 'info') {
    console.log(`[${type}] ${text}`);
    
    // 创建消息元素
    const message = document.createElement('div');
    message.className = `message-toast message-${type}`;
    message.textContent = text;
    
    // 添加样式
    message.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 12px 20px;
        border-radius: 4px;
        color: white;
        font-size: 14px;
        z-index: 10000;
        min-width: 200px;
        box-shadow: 0 2px 8px rgba(0,0,0,0.2);
        transition: all 0.3s ease;
    `;
    
    // 根据类型设置背景色
    switch(type) {
        case 'success':
            message.style.backgroundColor = '#4caf50';
            break;
        case 'error':
            message.style.backgroundColor = '#f44336';
            break;
        case 'warning':
            message.style.backgroundColor = '#ff9800';
            break;
        default:
            message.style.backgroundColor = '#2196f3';
    }
    
    // 添加到页面
    document.body.appendChild(message);
    
    // 3秒后自动移除
    setTimeout(() => {
        message.style.opacity = '0';
        message.style.transform = 'translateX(100%)';
        setTimeout(() => {
            if (message.parentNode) {
                message.parentNode.removeChild(message);
            }
        }, 300);
    }, 3000);
}

// ========== 将所有函数暴露到全局作用域 ==========
console.log('开始暴露函数到全局作用域...');

// 主要功能函数
window.initializePage = initializePage;
window.generateMockData = generateMockData;
window.loadTaskList = loadTaskList;
window.renderTaskList = renderTaskList;
window.applyFilters = applyFilters;

// 任务操作函数
window.showAddTaskModal = showAddTaskModal;
window.closeAddTaskModal = closeAddTaskModal;
window.saveTask = saveTask;
window.onProductSelect = onProductSelect;
window.showTaskDetail = showTaskDetail;
window.editTask = editTask;
window.removeTask = removeTask;

// 搜索和分页函数
window.searchTasks = searchTasks;
window.clearFilters = clearFilters;
window.handleSearch = handleSearch;
window.goToPage = goToPage;
window.handlePageSizeChange = handlePageSizeChange;

// 系统功能函数
window.syncTasks = syncTasks;
window.forceLoadData = forceLoadData;
window.startAutoRefresh = startAutoRefresh;
window.showMessage = showMessage;

console.log('所有函数已暴露到全局作用域'); 