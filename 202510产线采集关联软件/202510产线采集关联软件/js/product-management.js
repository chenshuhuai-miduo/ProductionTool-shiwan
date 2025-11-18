// 产品管理页面JavaScript功能

// 全局变量
let currentPage = 1;
let pageSize = 20;
let totalProducts = 0;
let filteredProducts = [];
let selectedProducts = [];
let currentProduct = null;
let currentTab = 'basic';
let wizardStep = 1;
let wizardData = {};

// 模拟产品数据
let mockProducts = [];

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('产品管理页面初始化...');
    initializePage();
});

function initializePage() {
    // 初始化事件监听
    initEventListeners();
    
    // 生成模拟数据
    generateMockData();
    
    // 加载产品列表
    loadProductList();
    
    // 更新时间显示
    updateTime();
    setInterval(updateTime, 1000);
    
    console.log('产品管理页面初始化完成');
}

function initEventListeners() {
    // 搜索输入框
    const productSearch = document.getElementById('productSearch');
    if (productSearch) {
        productSearch.addEventListener('input', debounce(handleSearch, 300));
        productSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchProducts();
            }
        });
    }
    
    // 分页大小选择
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', handlePageSizeChange);
    }
    
    // 全选复选框
    const selectAll = document.getElementById('selectAll');
    if (selectAll) {
        selectAll.addEventListener('change', handleSelectAll);
    }
    
    // ESC键关闭面板
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeProductDetail();
            closeConfirmDelete();
            closePackageWizard();
        }
    });
}

// 生成模拟数据
function generateMockData() {
    mockProducts = [
        {
            id: 1,
            productCode: 'PRD001',
            productName: '矿泉水',
            specification: '500ml',
            packageRatio: '24',
            unit: '瓶',
            status: '启用',
            validDays: 365,
            description: '天然矿泉水，清甜甘冽',
            createTime: '2024-01-15 10:30:00',
            creator: '张三'
        },
        {
            id: 2,
            productCode: 'PRD002',
            productName: '牛奶',
            specification: '250ml',
            packageRatio: '12:48',
            unit: '盒',
            status: '启用',
            validDays: 30,
            description: '纯牛奶，营养丰富',
            createTime: '2024-01-16 14:20:00',
            creator: '李四'
        },
        {
            id: 3,
            productCode: 'PRD003',
            productName: '酸奶',
            specification: '180ml',
            packageRatio: '6:12:48',
            unit: '杯',
            status: '启用',
            validDays: 21,
            description: '益生菌酸奶，助消化',
            createTime: '2024-01-17 09:15:00',
            creator: '王五'
        },
        {
            id: 4,
            productCode: 'PRD004',
            productName: '果汁',
            specification: '300ml',
            packageRatio: '20',
            unit: '瓶',
            status: '禁用',
            validDays: 180,
            description: '100%纯果汁，无添加',
            createTime: '2024-01-18 16:45:00',
            creator: '赵六'
        },
        {
            id: 5,
            productCode: 'PRD005',
            productName: '茶饮料',
            specification: '500ml',
            packageRatio: '15:36',
            unit: '瓶',
            status: '草稿',
            validDays: 270,
            description: '绿茶饮料，清香怡人',
            createTime: '2024-01-19 11:30:00',
            creator: '孙七'
        },
        {
            id: 6,
            productCode: 'PRD006',
            productName: '咖啡',
            specification: '330ml',
            packageRatio: '18',
            unit: '瓶',
            status: '启用',
            validDays: 365,
            description: '现磨咖啡，香浓醇厚',
            createTime: '2024-01-20 08:45:00',
            creator: '陈八'
        },
        {
            id: 7,
            productCode: 'PRD007',
            productName: '豆浆',
            specification: '250ml',
            packageRatio: '8:24',
            unit: '盒',
            status: '启用',
            validDays: 7,
            description: '新鲜豆浆，营养健康',
            createTime: '2024-01-21 07:20:00',
            creator: '刘九'
        },
        {
            id: 8,
            productCode: 'PRD008',
            productName: '功能饮料',
            specification: '250ml',
            packageRatio: '4:6:36',
            unit: '瓶',
            status: '启用',
            validDays: 540,
            description: '运动功能饮料，补充电解质',
            createTime: '2024-01-22 15:10:00',
            creator: '周十'
        },
        {
            id: 9,
            productCode: 'PRD009',
            productName: '椰汁',
            specification: '245ml',
            packageRatio: '30',
            unit: '罐',
            status: '禁用',
            validDays: 720,
            description: '天然椰汁，清香甘甜',
            createTime: '2024-01-23 12:30:00',
            creator: '吴十一'
        },
        {
            id: 10,
            productCode: 'PRD010',
            productName: '苏打水',
            specification: '350ml',
            packageRatio: '6:8:32',
            unit: '瓶',
            status: '草稿',
            validDays: 365,
            description: '无糖苏打水，清爽解腻',
            createTime: '2024-01-24 16:50:00',
            creator: '郑十二'
        }
    ];
    
    console.log('生成模拟数据:', mockProducts.length, '个产品');
}

// 加载产品列表
function loadProductList() {
    console.log('加载产品列表...');
    applyFilters();
    renderProductList();
    renderPagination();
}

function applyFilters() {
    const searchValue = document.getElementById('productSearch')?.value.toLowerCase() || '';
    
    filteredProducts = mockProducts.filter(product => {
        if (searchValue && 
            !product.productCode.toLowerCase().includes(searchValue) &&
            !product.productName.toLowerCase().includes(searchValue) &&
            !product.specification.toLowerCase().includes(searchValue)) {
            return false;
        }
        return true;
    });
    
    totalProducts = filteredProducts.length;
    console.log('筛选后产品数量:', totalProducts);
}

function renderProductList() {
    const tbody = document.getElementById('productTableBody');
    const totalCountSpan = document.getElementById('totalCount');
    
    if (!tbody) {
        console.error('产品表格tbody未找到');
        return;
    }
    
    // 更新总数显示
    if (totalCountSpan) {
        totalCountSpan.textContent = totalProducts;
    }
    
    // 计算当前页数据
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = Math.min(startIndex + pageSize, totalProducts);
    const currentPageProducts = filteredProducts.slice(startIndex, endIndex);
    
    // 清空表格
    tbody.innerHTML = '';
    
    if (currentPageProducts.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-state">
                    <div class="empty-icon">📦</div>
                    <div class="empty-text">暂无产品数据</div>
                    <div class="empty-action">
                        <button class="btn btn-primary" onclick="showAddProductModal()">添加产品</button>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    // 渲染数据行
    currentPageProducts.forEach(product => {
        const row = createProductRow(product);
        tbody.appendChild(row);
    });
}

function createProductRow(product) {
    const row = document.createElement('tr');
    row.dataset.productId = product.id;
    
    // 根据状态添加行样式
    if (product.status === '禁用') {
        row.style.opacity = '0.6';
    }
    
    row.innerHTML = `
        <td>
            <input type="checkbox" class="product-checkbox" value="${product.id}" onchange="handleProductSelect(this)">
        </td>
        <td>
            <a href="javascript:void(0)" onclick="showProductDetail(${product.id})" class="product-link">
                ${product.productCode}
            </a>
        </td>
        <td>${product.productName}</td>
        <td>${product.specification || '-'}</td>
        <td>${createPackageRatioTag(product.packageRatio)}</td>
        <td>${product.unit}</td>
        <td>
            <div class="action-buttons">
                ${createActionButtons(product)}
            </div>
        </td>
    `;
    
    return row;
}

function createPackageRatioTag(ratio) {
    if (!ratio) return '-';
    
    const parts = ratio.split(':');
    let level = parts.length + 1; // 2级、3级、4级关联
    let className = `package-tag level-${level}`;
    let text = '';
    
    switch(level) {
        case 2:
            text = '二级关联';
            break;
        case 3:
            text = '三级关联';
            break;
        case 4:
            text = '四级关联';
            break;
        default:
            text = '未知';
    }
    
    return `<span class="${className}">${text}</span>`;
}

function createActionButtons(product) {
    let buttons = [];
    
    // 编辑按钮
    buttons.push(`<button class="action-btn edit" title="编辑产品" onclick="editProduct(${product.id})">✏️</button>`);
    
    // 复制按钮
    buttons.push(`<button class="action-btn copy" title="复制产品" onclick="copyProduct(${product.id})">📋</button>`);
    
    // 删除按钮
    buttons.push(`<button class="action-btn delete" title="删除产品" onclick="showDeleteConfirm(${product.id})">🗑️</button>`);
    
    return buttons.join('');
}

// 分页功能
function renderPagination() {
    const paginationControls = document.getElementById('paginationControls');
    if (!paginationControls) return;
    
    const totalPages = Math.ceil(totalProducts / pageSize);
    
    if (totalPages <= 1) {
        paginationControls.innerHTML = '';
        return;
    }
    
    let paginationHTML = '';
    
    // 首页按钮
    paginationHTML += `<button class="pagination-btn ${currentPage === 1 ? 'disabled' : ''}" ${currentPage === 1 ? 'disabled' : ''} onclick="goToPage(1)">首页</button>`;
    
    // 上一页按钮
    paginationHTML += `<button class="pagination-btn ${currentPage === 1 ? 'disabled' : ''}" ${currentPage === 1 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})">上一页</button>`;
    
    // 页码按钮
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        paginationHTML += `<button class="pagination-btn ${i === currentPage ? 'active' : ''}" onclick="goToPage(${i})">${i}</button>`;
    }
    
    // 下一页按钮
    paginationHTML += `<button class="pagination-btn ${currentPage === totalPages ? 'disabled' : ''}" ${currentPage === totalPages ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})">下一页</button>`;
    
    // 末页按钮
    paginationHTML += `<button class="pagination-btn ${currentPage === totalPages ? 'disabled' : ''}" ${currentPage === totalPages ? 'disabled' : ''} onclick="goToPage(${totalPages})">末页</button>`;
    
    paginationControls.innerHTML = paginationHTML;
}

function goToPage(page) {
    if (page < 1 || page > Math.ceil(totalProducts / pageSize)) return;
    currentPage = page;
    renderProductList();
    renderPagination();
}

function handlePageSizeChange() {
    const pageSizeSelect = document.getElementById('pageSizeSelect');
    pageSize = parseInt(pageSizeSelect.value);
    currentPage = 1;
    renderProductList();
    renderPagination();
}

// 搜索功能
function handleSearch() {
    currentPage = 1;
    loadProductList();
}

function searchProducts() {
    handleSearch();
}

// 选择功能
function handleSelectAll() {
    const selectAll = document.getElementById('selectAll');
    const checkboxes = document.querySelectorAll('.product-checkbox');
    
    checkboxes.forEach(checkbox => {
        checkbox.checked = selectAll.checked;
    });
    
    updateSelectedProducts();
}

function handleProductSelect(checkbox) {
    updateSelectedProducts();
    
    // 更新全选状态
    const selectAll = document.getElementById('selectAll');
    const checkboxes = document.querySelectorAll('.product-checkbox');
    const checkedBoxes = document.querySelectorAll('.product-checkbox:checked');
    
    selectAll.checked = checkboxes.length === checkedBoxes.length;
    selectAll.indeterminate = checkedBoxes.length > 0 && checkedBoxes.length < checkboxes.length;
}

function updateSelectedProducts() {
    const checkedBoxes = document.querySelectorAll('.product-checkbox:checked');
    selectedProducts = Array.from(checkedBoxes).map(cb => parseInt(cb.value));
    console.log('选中的产品:', selectedProducts);
}

// 产品详情面板
function showProductDetail(productId) {
    const product = mockProducts.find(p => p.id === productId);
    if (!product) return;
    
    currentProduct = { ...product }; // 创建副本用于编辑
    
    const panel = document.getElementById('productDetailPanel');
    const panelTitle = document.getElementById('panelTitle');
    
    panelTitle.textContent = productId ? `编辑产品 - ${product.productCode}` : '添加产品';
    
    // 显示删除按钮（仅编辑时）
    const deleteBtn = document.getElementById('deleteBtn');
    if (deleteBtn) {
        deleteBtn.style.display = productId ? 'inline-block' : 'none';
    }
    
    // 切换到基本信息标签页
    switchTab('basic');
    
    // 显示面板
    panel.classList.add('show');
}

function showAddProductModal() {
    console.log('showAddProductModal called');
    
    currentProduct = {
        id: null,
        productCode: '',
        productName: '',
        specification: '',
        packageRatio: '',
        unit: '瓶',
        status: '草稿',
        validDays: 365,
        description: '',
        createTime: new Date().toLocaleString('zh-CN'),
        creator: '当前用户'
    };
    
    const panel = document.getElementById('productDetailPanel');
    const panelTitle = document.getElementById('panelTitle');
    
    console.log('Panel element:', panel);
    console.log('Panel title element:', panelTitle);
    
    if (!panel) {
        console.error('产品详情面板元素未找到');
        return;
    }
    
    if (panelTitle) {
        panelTitle.textContent = '添加产品';
    }
    
    // 隐藏删除按钮
    const deleteBtn = document.getElementById('deleteBtn');
    if (deleteBtn) {
        deleteBtn.style.display = 'none';
    }
    
    // 切换到基本信息标签页
    switchTab('basic');
    
    // 显示面板 - 使用setTimeout确保DOM更新完成
    setTimeout(() => {
        panel.classList.add('show');
        console.log('Panel classes after show:', panel.className);
    }, 10);
}

function closeProductDetail() {
    const panel = document.getElementById('productDetailPanel');
    panel.classList.remove('show');
    currentProduct = null;
}

function switchTab(tabName) {
    currentTab = tabName;
    
    // 更新标签页状态
    document.querySelectorAll('.panel-tabs .tab-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.dataset.tab === tabName) {
            btn.classList.add('active');
        }
    });
    
    // 显示对应内容
    const panelContent = document.getElementById('panelContent');
    
    switch (tabName) {
        case 'basic':
            panelContent.innerHTML = createBasicInfoContent();
            break;
        case 'packaging':
            panelContent.innerHTML = createPackagingContent();
            break;
    }
}

function createBasicInfoContent() {
    return `
        <div class="product-form">
            <div class="form-group">
                <label class="form-label">产品编号 <span style="color: red;">*</span></label>
                <input type="text" class="form-control" id="productCode" value="${currentProduct.productCode}" placeholder="请输入产品编号">
                <div class="form-error" id="productCodeError"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label">产品名称 <span style="color: red;">*</span></label>
                <input type="text" class="form-control" id="productName" value="${currentProduct.productName}" placeholder="请输入产品名称">
                <div class="form-error" id="productNameError"></div>
            </div>
            
            <div class="form-group">
                <label class="form-label">规格</label>
                <input type="text" class="form-control" id="specification" value="${currentProduct.specification || ''}" placeholder="请输入产品规格">
            </div>
            
            <div class="form-group">
                <label class="form-label">计量单位 <span style="color: red;">*</span></label>
                <select class="form-control" id="unit">
                    <option value="瓶" ${currentProduct.unit === '瓶' ? 'selected' : ''}>瓶</option>
                    <option value="盒" ${currentProduct.unit === '盒' ? 'selected' : ''}>盒</option>
                    <option value="箱" ${currentProduct.unit === '箱' ? 'selected' : ''}>箱</option>
                    <option value="个" ${currentProduct.unit === '个' ? 'selected' : ''}>个</option>
                    <option value="杯" ${currentProduct.unit === '杯' ? 'selected' : ''}>杯</option>
                    <option value="袋" ${currentProduct.unit === '袋' ? 'selected' : ''}>袋</option>
                </select>
            </div>
            
            <div class="form-group">
                <label class="form-label">保质期天数</label>
                <input type="number" class="form-control" id="validDays" value="${currentProduct.validDays || 365}" min="1" placeholder="请输入保质期天数">
            </div>
            
            <div class="form-group">
                <label class="form-label">产品描述</label>
                <textarea class="form-control" id="description" rows="3" placeholder="请输入产品描述">${currentProduct.description || ''}</textarea>
            </div>
        </div>
    `;
}

function createPackagingContent() {
    return `
        <div class="packaging-config">
            <div class="config-section">
                <h4>包装比例配置</h4>
                <div class="package-input-group">
                    <label>包装比例：</label>
                    <input type="text" class="package-input" id="packageRatio" value="${currentProduct.packageRatio || ''}" placeholder="如：24 或 12:48">
                    <button class="wizard-btn" onclick="showPackageWizard()">配置向导</button>
                </div>
                <div style="font-size: 10px; color: #666; margin-top: 4px;">
                    格式说明：二级关联(24)，三级关联(12:48)，四级关联(6:12:48)
                </div>
            </div>
            
            <div class="config-section">
                <h4>层级预览</h4>
                <div class="level-preview" id="levelPreview">
                    ${createLevelPreview(currentProduct.packageRatio)}
                </div>
            </div>
        </div>
    `;
}

function createLevelPreview(ratio) {
    if (!ratio) {
        return '<div style="color: #999; font-size: 11px;">请配置包装比例以查看层级预览</div>';
    }
    
    const parts = ratio.split(':');
    let preview = '<div class="level-tree">';
    
    if (parts.length === 1) {
        // 二级关联
        preview += `
            <div class="level-item level-1">虚拟垛标 (1个)</div>
            <div class="level-item level-2">└─ 单品 (${parts[0]}个)</div>
        `;
    } else if (parts.length === 2) {
        // 三级关联
        const [box, pallet] = parts;
        preview += `
            <div class="level-item level-1">虚拟垛标 (1个)</div>
            <div class="level-item level-2">├─ 箱码 (${pallet}个)</div>
            <div class="level-item level-3">│   └─ 单品 (${box}个/箱)</div>
            <div class="level-item level-3">└─ 总计单品：${parseInt(box) * parseInt(pallet)}个</div>
        `;
    } else if (parts.length === 3) {
        // 四级关联
        const [item, box, pallet] = parts;
        preview += `
            <div class="level-item level-1">虚拟垛标 (1个)</div>
            <div class="level-item level-2">├─ 箱码 (${pallet}个)</div>
            <div class="level-item level-3">│   ├─ 🏷️ 盒码 (${box}个/箱)</div>
            <div class="level-item level-4">│   │   └─ 📄 单品 (${item}个/盒)</div>
            <div class="level-item level-4">└─ 总计单品：${parseInt(item) * parseInt(box) * parseInt(pallet)}个</div>
        `;
    }
    
    preview += '</div>';
    return preview;
}

// 包装配置向导
function showPackageWizard() {
    wizardStep = 1;
    wizardData = {};
    
    const modal = document.getElementById('packageWizardModal');
    modal.style.display = 'flex';
    
    updateWizardStep();
}

function closePackageWizard() {
    const modal = document.getElementById('packageWizardModal');
    modal.style.display = 'none';
    wizardStep = 1;
    wizardData = {};
}

function selectPackageLevel(level) {
    wizardData.level = level;
    console.log('选择包装层级:', level);
}

function wizardNextStep() {
    if (wizardStep === 1) {
        if (!wizardData.level) {
            showMessage('请选择包装层级', 'warning');
            return;
        }
        wizardStep = 2;
        updateWizardStep();
    }
}

function wizardPrevStep() {
    if (wizardStep === 2) {
        wizardStep = 1;
        updateWizardStep();
    }
}

function updateWizardStep() {
    const step1 = document.getElementById('wizardStep1');
    const step2 = document.getElementById('wizardStep2');
    const prevBtn = document.getElementById('wizardPrevBtn');
    const nextBtn = document.getElementById('wizardNextBtn');
    const finishBtn = document.getElementById('wizardFinishBtn');
    
    if (wizardStep === 1) {
        step1.style.display = 'block';
        step2.style.display = 'none';
        prevBtn.style.display = 'none';
        nextBtn.style.display = 'inline-block';
        finishBtn.style.display = 'none';
    } else if (wizardStep === 2) {
        step1.style.display = 'none';
        step2.style.display = 'block';
        prevBtn.style.display = 'inline-block';
        nextBtn.style.display = 'none';
        finishBtn.style.display = 'inline-block';
        
        // 生成配置界面
        generateRatioConfig();
    }
}

function generateRatioConfig() {
    const ratioConfig = document.getElementById('ratioConfig');
    const ratioPreview = document.getElementById('ratioPreview');
    
    let configHTML = '';
    
    if (wizardData.level === 2) {
        configHTML = `
            <div class="ratio-input-group">
                <label>单品数量：</label>
                <input type="number" id="ratio1" min="1" value="24" onchange="updateRatioPreview()">
                <span>个单品组成1个虚拟垛标</span>
            </div>
        `;
    } else if (wizardData.level === 3) {
        configHTML = `
            <div class="ratio-input-group">
                <label>单品数量：</label>
                <input type="number" id="ratio1" min="1" value="12" onchange="updateRatioPreview()">
                <span>个单品组成1箱</span>
            </div>
            <div class="ratio-input-group">
                <label>箱数量：</label>
                <input type="number" id="ratio2" min="1" value="48" onchange="updateRatioPreview()">
                <span>箱组成1个虚拟垛标</span>
            </div>
        `;
    } else if (wizardData.level === 4) {
        configHTML = `
            <div class="ratio-input-group">
                <label>单品数量：</label>
                <input type="number" id="ratio1" min="1" value="6" onchange="updateRatioPreview()">
                <span>个单品组成1盒</span>
            </div>
            <div class="ratio-input-group">
                <label>盒数量：</label>
                <input type="number" id="ratio2" min="1" value="12" onchange="updateRatioPreview()">
                <span>盒组成1箱</span>
            </div>
            <div class="ratio-input-group">
                <label>箱数量：</label>
                <input type="number" id="ratio3" min="1" value="48" onchange="updateRatioPreview()">
                <span>箱组成1个虚拟垛标</span>
            </div>
        `;
    }
    
    ratioConfig.innerHTML = configHTML;
    
    // 初始化预览
    setTimeout(() => {
        updateRatioPreview();
    }, 100);
}

function updateRatioPreview() {
    const ratioPreview = document.getElementById('ratioPreview');
    const ratio1 = document.getElementById('ratio1')?.value || '';
    const ratio2 = document.getElementById('ratio2')?.value || '';
    const ratio3 = document.getElementById('ratio3')?.value || '';
    
    let previewHTML = '<h5>层级预览：</h5><div class="preview-tree">';
    
    if (wizardData.level === 2) {
        previewHTML += `
            <div class="preview-item">📦 虚拟垛标 (1个)</div>
            <div class="preview-item highlight">└─ 📄 单品 (${ratio1}个)</div>
        `;
    } else if (wizardData.level === 3) {
        const total = parseInt(ratio1) * parseInt(ratio2);
        previewHTML += `
            <div class="preview-item">📦 虚拟垛标 (1个)</div>
            <div class="preview-item">├─ 📋 箱码 (${ratio2}个)</div>
            <div class="preview-item highlight">│   └─ 📄 单品 (${ratio1}个/箱)</div>
            <div class="preview-item">└─ 总计单品：${total}个</div>
        `;
    } else if (wizardData.level === 4) {
        const total = parseInt(ratio1) * parseInt(ratio2) * parseInt(ratio3);
        previewHTML += `
            <div class="preview-item">📦 虚拟垛标 (1个)</div>
            <div class="preview-item">├─ 📋 箱码 (${ratio3}个)</div>
            <div class="preview-item">│   ├─ 🏷️ 盒码 (${ratio2}个/箱)</div>
            <div class="preview-item highlight">│   │   └─ 📄 单品 (${ratio1}个/盒)</div>
            <div class="preview-item">└─ 总计单品：${total}个</div>
        `;
    }
    
    previewHTML += '</div>';
    ratioPreview.innerHTML = previewHTML;
}

function finishWizard() {
    const ratio1 = document.getElementById('ratio1')?.value || '';
    const ratio2 = document.getElementById('ratio2')?.value || '';
    const ratio3 = document.getElementById('ratio3')?.value || '';
    
    let ratioString = '';
    
    if (wizardData.level === 2) {
        ratioString = ratio1;
    } else if (wizardData.level === 3) {
        ratioString = `${ratio1}:${ratio2}`;
    } else if (wizardData.level === 4) {
        ratioString = `${ratio1}:${ratio2}:${ratio3}`;
    }
    
    // 更新包装比例输入框
    const packageRatioInput = document.getElementById('packageRatio');
    if (packageRatioInput) {
        packageRatioInput.value = ratioString;
        currentProduct.packageRatio = ratioString;
        
        // 更新预览
        const levelPreview = document.getElementById('levelPreview');
        if (levelPreview) {
            levelPreview.innerHTML = createLevelPreview(ratioString);
        }
    }
    
    closePackageWizard();
    showMessage('包装配置完成', 'success');
}

// 产品操作
function editProduct(productId) {
    showProductDetail(productId);
}

function copyProduct(productId) {
    const product = mockProducts.find(p => p.id === productId);
    if (!product) return;
    
    const newProduct = {
        ...product,
        id: Math.max(...mockProducts.map(p => p.id)) + 1,
        productCode: product.productCode + '_copy',
        productName: product.productName + ' (副本)',
        status: '草稿',
        createTime: new Date().toLocaleString('zh-CN'),
        creator: '当前用户'
    };
    
    mockProducts.unshift(newProduct);
    loadProductList();
    showMessage(`产品 ${product.productName} 复制成功`, 'success');
}

function showDeleteConfirm(productId) {
    const product = mockProducts.find(p => p.id === productId);
    if (!product) return;
    
    const modal = document.getElementById('confirmDeleteModal');
    const productNameSpan = document.getElementById('deleteProductName');
    
    productNameSpan.textContent = product.productName;
    modal.dataset.productId = productId;
    modal.style.display = 'flex';
}

function closeConfirmDelete() {
    const modal = document.getElementById('confirmDeleteModal');
    modal.style.display = 'none';
    delete modal.dataset.productId;
}

function confirmDelete() {
    const modal = document.getElementById('confirmDeleteModal');
    const productId = parseInt(modal.dataset.productId);
    
    if (productId) {
        const index = mockProducts.findIndex(p => p.id === productId);
        if (index !== -1) {
            const product = mockProducts[index];
            mockProducts.splice(index, 1);
            loadProductList();
            showMessage(`产品 ${product.productName} 删除成功`, 'success');
        }
    }
    
    closeConfirmDelete();
}

function deleteProduct() {
    if (!currentProduct || !currentProduct.id) return;
    showDeleteConfirm(currentProduct.id);
}

function saveProduct() {
    // 收集表单数据
    const productCode = document.getElementById('productCode')?.value.trim();
    const productName = document.getElementById('productName')?.value.trim();
    const specification = document.getElementById('specification')?.value.trim();
    const unit = document.getElementById('unit')?.value;
    const validDays = parseInt(document.getElementById('validDays')?.value) || 365;
    const description = document.getElementById('description')?.value.trim();
    const packageRatio = document.getElementById('packageRatio')?.value.trim();
    
    // 验证必填字段
    let hasError = false;
    
    if (!productCode) {
        showFieldError('productCodeError', '产品编号不能为空');
        hasError = true;
    } else {
        // 检查产品编号唯一性
        const existingProduct = mockProducts.find(p => p.productCode === productCode && p.id !== currentProduct.id);
        if (existingProduct) {
            showFieldError('productCodeError', '产品编号已存在');
            hasError = true;
        } else {
            clearFieldError('productCodeError');
        }
    }
    
    if (!productName) {
        showFieldError('productNameError', '产品名称不能为空');
        hasError = true;
    } else {
        clearFieldError('productNameError');
    }
    
    if (hasError) {
        return;
    }
    
    // 更新产品数据
    currentProduct.productCode = productCode;
    currentProduct.productName = productName;
    currentProduct.specification = specification;
    currentProduct.unit = unit;
    currentProduct.validDays = validDays;
    currentProduct.description = description;
    currentProduct.packageRatio = packageRatio;
    
    if (currentProduct.id) {
        // 更新现有产品
        const index = mockProducts.findIndex(p => p.id === currentProduct.id);
        if (index !== -1) {
            mockProducts[index] = { ...currentProduct };
            showMessage(`产品 ${productName} 更新成功`, 'success');
        }
    } else {
        // 添加新产品
        currentProduct.id = Math.max(...mockProducts.map(p => p.id)) + 1;
        currentProduct.status = '草稿';
        currentProduct.createTime = new Date().toLocaleString('zh-CN');
        currentProduct.creator = '当前用户';
        
        mockProducts.unshift({ ...currentProduct });
        showMessage(`产品 ${productName} 添加成功`, 'success');
    }
    
    closeProductDetail();
    loadProductList();
}

function showFieldError(errorId, message) {
    const errorElement = document.getElementById(errorId);
    if (errorElement) {
        errorElement.textContent = message;
    }
}

function clearFieldError(errorId) {
    const errorElement = document.getElementById(errorId);
    if (errorElement) {
        errorElement.textContent = '';
    }
}

// 刷新数据
function refreshProducts() {
    showMessage('正在刷新产品数据...', 'info');
    loadProductList();
    showMessage('产品数据刷新完成', 'success');
}

// 工具函数
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
window.showAddProductModal = showAddProductModal;
window.refreshProducts = refreshProducts;
window.searchProducts = searchProducts;
window.handleSelectAll = handleSelectAll;
window.handleProductSelect = handleProductSelect;
window.handlePageSizeChange = handlePageSizeChange;
window.goToPage = goToPage;
window.showProductDetail = showProductDetail;
window.closeProductDetail = closeProductDetail;
window.switchTab = switchTab;
window.editProduct = editProduct;
window.copyProduct = copyProduct;
window.showDeleteConfirm = showDeleteConfirm;
window.closeConfirmDelete = closeConfirmDelete;
window.confirmDelete = confirmDelete;
window.deleteProduct = deleteProduct;
window.saveProduct = saveProduct;
window.showPackageWizard = showPackageWizard;
window.closePackageWizard = closePackageWizard;
window.selectPackageLevel = selectPackageLevel;
window.wizardNextStep = wizardNextStep;
window.wizardPrevStep = wizardPrevStep;
window.updateRatioPreview = updateRatioPreview;
window.finishWizard = finishWizard;
window.loadProductList = loadProductList;

console.log('所有全局函数已导出'); 