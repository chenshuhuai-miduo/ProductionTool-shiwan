// 产品管理页面JavaScript功能 - 调试版本

console.log('产品管理调试脚本开始加载...');

// 全局变量
let currentPage = 1;
let pageSize = 20;
let totalProducts = 0;
let filteredProducts = [];
let selectedProducts = [];
let currentProduct = null;
let currentTab = 'basic';

// 模拟产品数据
let mockProducts = [
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

console.log('模拟数据已加载:', mockProducts.length, '个产品');

// 页面初始化
function initializePage() {
    console.log('开始初始化页面...');
    
    try {
        // 加载产品列表
        loadProductList();
        
        // 更新时间显示
        updateTime();
        setInterval(updateTime, 1000);
        
        console.log('页面初始化完成');
    } catch (error) {
        console.error('页面初始化失败:', error);
    }
}

// 加载产品列表
function loadProductList() {
    console.log('加载产品列表...');
    
    try {
        filteredProducts = [...mockProducts];
        totalProducts = filteredProducts.length;
        
        console.log('筛选后产品数量:', totalProducts);
        
        renderProductList();
        renderPagination();
        
        console.log('产品列表加载完成');
    } catch (error) {
        console.error('加载产品列表失败:', error);
    }
}

// 渲染产品列表
function renderProductList() {
    console.log('渲染产品列表...');
    
    const tbody = document.getElementById('productTableBody');
    const totalCountSpan = document.getElementById('totalCount');
    
    if (!tbody) {
        console.error('未找到产品表格tbody元素');
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
    
    console.log('当前页产品数量:', currentPageProducts.length);
    
    // 清空表格
    tbody.innerHTML = '';
    
    if (currentPageProducts.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 40px; color: #999;">
                    <div>📦</div>
                    <div>暂无产品数据</div>
                    <div style="margin-top: 10px;">
                        <button onclick="showAddProductModal()" style="padding: 8px 16px; background: #1890ff; color: white; border: none; border-radius: 4px; cursor: pointer;">添加产品</button>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    // 渲染数据行
    currentPageProducts.forEach((product, index) => {
        console.log('渲染产品:', product.productName);
        
        const row = document.createElement('tr');
        row.dataset.productId = product.id;
        
        // 根据状态添加行样式
        if (product.status === '禁用') {
            row.style.opacity = '0.6';
        }
        
        row.innerHTML = `
            <td>
                <input type="checkbox" class="product-checkbox" value="${product.id}">
            </td>
            <td>
                <a href="javascript:void(0)" onclick="showProductDetail(${product.id})" style="color: #1890ff; text-decoration: none;">
                    ${product.productCode}
                </a>
            </td>
            <td>${product.productName}</td>
            <td>${product.specification || '-'}</td>
            <td>${createPackageRatioTag(product.packageRatio)}</td>
            <td>${product.unit}</td>
            <td>
                <button onclick="editProduct(${product.id})" style="margin: 0 2px; padding: 2px 6px; font-size: 12px; border: 1px solid #ccc; background: #f0f0f0; cursor: pointer;" title="编辑">✏️</button>
                <button onclick="copyProduct(${product.id})" style="margin: 0 2px; padding: 2px 6px; font-size: 12px; border: 1px solid #ccc; background: #f0f0f0; cursor: pointer;" title="复制">📋</button>
                <button onclick="deleteProduct(${product.id})" style="margin: 0 2px; padding: 2px 6px; font-size: 12px; border: 1px solid #ccc; background: #f0f0f0; cursor: pointer;" title="删除">🗑️</button>
            </td>
        `;
        
        tbody.appendChild(row);
    });
    
    console.log('产品列表渲染完成');
}

// 创建包装比例标签
function createPackageRatioTag(ratio) {
    if (!ratio) return '-';
    
    const parts = ratio.split(':');
    let level = parts.length + 1;
    let text = '';
    let color = '';
    
    switch(level) {
        case 2:
            text = '二级关联';
            color = '#1890ff';
            break;
        case 3:
            text = '三级关联';
            color = '#52c41a';
            break;
        case 4:
            text = '四级关联';
            color = '#722ed1';
            break;
        default:
            text = '未知';
            color = '#999';
    }
    
    return `<span style="padding: 2px 6px; border-radius: 10px; font-size: 10px; font-weight: bold; background: ${color}20; color: ${color}; border: 1px solid ${color}40;">${text}</span>`;
}

// 渲染分页
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
    paginationHTML += `<button ${currentPage === 1 ? 'disabled' : ''} onclick="goToPage(1)" style="margin: 0 2px; padding: 4px 8px; font-size: 10px; border: 1px solid #999; background: #f0f0f0; cursor: pointer;">首页</button>`;
    
    // 上一页按钮
    paginationHTML += `<button ${currentPage === 1 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})" style="margin: 0 2px; padding: 4px 8px; font-size: 10px; border: 1px solid #999; background: #f0f0f0; cursor: pointer;">上一页</button>`;
    
    // 页码按钮
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        const isActive = i === currentPage;
        paginationHTML += `<button onclick="goToPage(${i})" style="margin: 0 2px; padding: 4px 8px; font-size: 10px; border: 1px solid #999; background: ${isActive ? '#4a90e2' : '#f0f0f0'}; color: ${isActive ? 'white' : '#333'}; cursor: pointer;">${i}</button>`;
    }
    
    // 下一页按钮
    paginationHTML += `<button ${currentPage === totalPages ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})" style="margin: 0 2px; padding: 4px 8px; font-size: 10px; border: 1px solid #999; background: #f0f0f0; cursor: pointer;">下一页</button>`;
    
    // 末页按钮
    paginationHTML += `<button ${currentPage === totalPages ? 'disabled' : ''} onclick="goToPage(${totalPages})" style="margin: 0 2px; padding: 4px 8px; font-size: 10px; border: 1px solid #999; background: #f0f0f0; cursor: pointer;">末页</button>`;
    
    paginationControls.innerHTML = paginationHTML;
}

// 分页跳转
function goToPage(page) {
    if (page < 1 || page > Math.ceil(totalProducts / pageSize)) return;
    currentPage = page;
    renderProductList();
    renderPagination();
}

// 显示添加产品模态框
function showAddProductModal() {
    console.log('显示添加产品模态框');
    alert('添加产品功能开发中...\n\n当前已有 ' + mockProducts.length + ' 个产品示例：\n' + 
          mockProducts.map(p => `${p.productCode} - ${p.productName}`).join('\n'));
}

// 显示产品详情
function showProductDetail(productId) {
    const product = mockProducts.find(p => p.id === productId);
    if (!product) return;
    
    alert(`产品详情：\n\n` +
          `产品编号：${product.productCode}\n` +
          `产品名称：${product.productName}\n` +
          `规格：${product.specification}\n` +
          `包装比例：${product.packageRatio}\n` +
          `计量单位：${product.unit}\n` +
          `状态：${product.status}\n` +
          `描述：${product.description}`);
}

// 编辑产品
function editProduct(productId) {
    console.log('编辑产品:', productId);
    alert('编辑产品功能开发中...');
}

// 复制产品
function copyProduct(productId) {
    const product = mockProducts.find(p => p.id === productId);
    if (!product) return;
    
    console.log('复制产品:', product.productName);
    alert(`已复制产品：${product.productName}\n复制功能开发中...`);
}

// 删除产品
function deleteProduct(productId) {
    const product = mockProducts.find(p => p.id === productId);
    if (!product) return;
    
    if (confirm(`确定要删除产品 "${product.productName}" 吗？`)) {
        const index = mockProducts.findIndex(p => p.id === productId);
        if (index !== -1) {
            mockProducts.splice(index, 1);
            loadProductList();
            alert('产品删除成功！');
        }
    }
}

// 刷新数据
function refreshProducts() {
    console.log('刷新产品数据');
    loadProductList();
    alert('数据刷新完成！');
}

// 搜索产品
function searchProducts() {
    const searchInput = document.getElementById('productSearch');
    const searchValue = searchInput ? searchInput.value.toLowerCase() : '';
    
    console.log('搜索产品:', searchValue);
    
    if (!searchValue) {
        filteredProducts = [...mockProducts];
    } else {
        filteredProducts = mockProducts.filter(product => 
            product.productCode.toLowerCase().includes(searchValue) ||
            product.productName.toLowerCase().includes(searchValue) ||
            (product.specification && product.specification.toLowerCase().includes(searchValue))
        );
    }
    
    totalProducts = filteredProducts.length;
    currentPage = 1;
    renderProductList();
    renderPagination();
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

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM加载完成，开始初始化...');
    setTimeout(initializePage, 100);
});

// 备用初始化
window.addEventListener('load', function() {
    console.log('页面加载完成，备用初始化...');
    if (document.getElementById('productTableBody').children.length === 0) {
        setTimeout(initializePage, 200);
    }
});

// 全局函数导出
window.showAddProductModal = showAddProductModal;
window.refreshProducts = refreshProducts;
window.searchProducts = searchProducts;
window.goToPage = goToPage;
window.showProductDetail = showProductDetail;
window.editProduct = editProduct;
window.copyProduct = copyProduct;
window.deleteProduct = deleteProduct;

console.log('产品管理调试脚本加载完成'); 