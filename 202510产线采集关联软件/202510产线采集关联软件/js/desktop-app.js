// 桌面应用JavaScript功能

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

function initializeApp() {
    // 更新时间显示
    updateTime();
    setInterval(updateTime, 1000);
    
    // 初始化事件监听
    initEventListeners();
    
    // 初始化树形菜单
    initTreeView();
    
    // 显示欢迎消息
    setTimeout(() => {
        showMessage('系统启动完成', 'info');
    }, 1000);
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
    // 标题栏按钮
    const minimizeBtn = document.querySelector('.minimize-btn');
    const maximizeBtn = document.querySelector('.maximize-btn');
    const closeBtn = document.querySelector('.close-btn');
    
    if (minimizeBtn) {
        minimizeBtn.addEventListener('click', minimizeWindow);
    }
    
    if (maximizeBtn) {
        maximizeBtn.addEventListener('click', maximizeWindow);
    }
    
    if (closeBtn) {
        closeBtn.addEventListener('click', closeWindow);
    }
    
    // 菜单项事件
    const menuItems = document.querySelectorAll('.menu-option');
    menuItems.forEach(item => {
        item.addEventListener('click', handleMenuClick);
    });
}

function initTreeView() {
    // 树形菜单默认展开状态已在HTML中设置
    console.log('树形菜单初始化完成');
}

// 标题栏按钮功能
function minimizeWindow() {
    showMessage('最小化功能', 'info');
    // 在实际桌面应用中，这里会调用系统API最小化窗口
}

function maximizeWindow() {
    const app = document.querySelector('.desktop-app');
    if (app.classList.contains('maximized')) {
        app.classList.remove('maximized');
        showMessage('还原窗口', 'info');
    } else {
        app.classList.add('maximized');
        showMessage('最大化窗口', 'info');
    }
}

function closeWindow() {
    if (confirm('确定要退出应用程序吗？')) {
        showMessage('正在退出...', 'info');
        // 在实际桌面应用中，这里会关闭应用程序
        setTimeout(() => {
            window.close();
        }, 1000);
    }
}

// 菜单点击处理
function handleMenuClick(event) {
    const menuText = event.target.textContent;
    
    switch(menuText) {
        case '新建任务':
            openPage('task-create');
            break;
        case '打开任务':
            openPage('task-list');
            break;
        case '导入数据':
            showMessage('导入数据功能开发中...', 'info');
            break;
        case '导出数据':
            showMessage('导出数据功能开发中...', 'info');
            break;
        case '退出':
            closeWindow();
            break;
        case '刷新':
            refreshContent();
            break;
        case '全屏':
            toggleFullscreen();
            break;
        case '系统设置':
            showMessage('系统设置功能开发中...', 'info');
            break;
        case '设备配置':
            showMessage('设备配置功能开发中...', 'info');
            break;
        case '数据库连接':
            showMessage('数据库连接功能开发中...', 'info');
            break;
        case '使用手册':
            showMessage('使用手册功能开发中...', 'info');
            break;
        case '关于软件':
            showAboutDialog();
            break;
        default:
            showMessage(`点击了菜单：${menuText}`, 'info');
    }
}

// 树形菜单节点切换
function toggleNode(nodeHeader) {
    const treeNode = nodeHeader.parentElement;
    treeNode.classList.toggle('expanded');
}

// 页面导航功能
function openPage(pageName) {
    // 更新面包屑
    updateBreadcrumb(getPageTitle(pageName));
    
    // 在实际应用中，这里会加载对应的页面内容
    // 现在我们跳转到对应的HTML页面
    const pageUrl = `pages/${pageName}.html`;
    
    // 检查页面是否存在
    fetch(pageUrl, { method: 'HEAD' })
        .then(response => {
            if (response.ok) {
                window.location.href = pageUrl;
            } else {
                showMessage(`页面 ${getPageTitle(pageName)} 开发中...`, 'warning');
            }
        })
        .catch(() => {
            showMessage(`页面 ${getPageTitle(pageName)} 开发中...`, 'warning');
        });
}

function getPageTitle(pageName) {
    const titles = {
        'product-management': '产品管理',
        'task-create': '任务创建',
        'task-list': '任务列表',
        'task-select': '任务选择',
        'task-control': '执行控制',
        'code-collection': '码采集',
        'realtime-monitor': '实时监控',
        'code-query': '码查询',
        'data-upload': '数据上传'
    };
    return titles[pageName] || pageName;
}

function updateBreadcrumb(pageTitle) {
    const breadcrumb = document.querySelector('.breadcrumb');
    if (breadcrumb) {
        breadcrumb.innerHTML = `
            <span class="breadcrumb-item">首页</span>
            <span class="breadcrumb-separator"> > </span>
            <span class="breadcrumb-item">${pageTitle}</span>
        `;
    }
}

// 刷新内容
function refreshContent() {
    showMessage('正在刷新...', 'info');
    
    // 模拟刷新过程
    setTimeout(() => {
        // 更新状态卡片数据
        updateStatusCards();
        showMessage('刷新完成', 'success');
    }, 1000);
}

function updateStatusCards() {
    // 模拟更新数据
    const cards = document.querySelectorAll('.card-value');
    cards.forEach(card => {
        const currentValue = parseInt(card.textContent) || 0;
        const newValue = currentValue + Math.floor(Math.random() * 5) - 2;
        card.textContent = Math.max(0, newValue);
    });
}

// 全屏切换
function toggleFullscreen() {
    if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen().then(() => {
            showMessage('已进入全屏模式', 'info');
        });
    } else {
        document.exitFullscreen().then(() => {
            showMessage('已退出全屏模式', 'info');
        });
    }
}

// 关于对话框
function showAboutDialog() {
    const aboutContent = `
        <div style="text-align: center; padding: 20px;">
            <div style="font-size: 24px; margin-bottom: 16px;">📦</div>
            <h3>产线采集关联软件</h3>
            <p>版本：v1.0.0</p>
            <p>SQL ScanDuo</p>
            <br>
            <p>专业的产线码采集与关联管理系统</p>
            <p>支持多层级包装比例配置和自动适配采集模式</p>
            <br>
            <p>© 2024 产线采集关联软件. 保留所有权利.</p>
        </div>
    `;
    
    showDialog('关于软件', aboutContent);
}

// 消息提示功能
function showMessage(text, type = 'info') {
    // 创建消息元素
    const message = document.createElement('div');
    message.className = `desktop-message ${type}`;
    message.innerHTML = `
        <span class="message-icon">${getMessageIcon(type)}</span>
        <span class="message-text">${text}</span>
    `;
    
    // 添加到页面
    document.body.appendChild(message);
    
    // 自动消失
    setTimeout(() => {
        if (message.parentNode) {
            message.style.opacity = '0';
            setTimeout(() => {
                document.body.removeChild(message);
            }, 300);
        }
    }, 3000);
}

function getMessageIcon(type) {
    const icons = {
        'info': 'ℹ️',
        'success': '✅',
        'warning': '⚠️',
        'error': '❌'
    };
    return icons[type] || 'ℹ️';
}

// 对话框功能
function showDialog(title, content, buttons = null) {
    const dialog = document.createElement('div');
    dialog.className = 'desktop-dialog';
    
    const dialogContent = `
        <div class="dialog-content">
            <div class="dialog-header">
                <span class="dialog-title">${title}</span>
                <button class="dialog-close" onclick="closeDialog(this)">×</button>
            </div>
            <div class="dialog-body">
                ${content}
            </div>
            <div class="dialog-footer">
                ${buttons || '<button class="btn btn-primary" onclick="closeDialog(this)">确定</button>'}
            </div>
        </div>
    `;
    
    dialog.innerHTML = dialogContent;
    document.body.appendChild(dialog);
    
    return dialog;
}

function closeDialog(element) {
    const dialog = element.closest('.desktop-dialog');
    if (dialog) {
        document.body.removeChild(dialog);
    }
}

// 添加桌面应用特有的样式
const desktopStyles = document.createElement('style');
desktopStyles.textContent = `
    /* 最大化状态 */
    .desktop-app.maximized {
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw !important;
        height: 100vh !important;
        border: none;
    }
    
    /* 桌面消息样式 */
    .desktop-message {
        position: fixed;
        top: 60px;
        right: 20px;
        background: #f8f8f8;
        border: 1px solid #999;
        border-radius: 4px;
        padding: 8px 12px;
        font-size: 11px;
        box-shadow: 2px 2px 4px rgba(0,0,0,0.3);
        z-index: 2000;
        display: flex;
        align-items: center;
        gap: 6px;
        opacity: 1;
        transition: opacity 0.3s ease;
        max-width: 300px;
    }
    
    .desktop-message.success {
        background: #d4edda;
        border-color: #c3e6cb;
        color: #155724;
    }
    
    .desktop-message.warning {
        background: #fff3cd;
        border-color: #ffeaa7;
        color: #856404;
    }
    
    .desktop-message.error {
        background: #f8d7da;
        border-color: #f5c6cb;
        color: #721c24;
    }
    
    .desktop-message.info {
        background: #d1ecf1;
        border-color: #bee5eb;
        color: #0c5460;
    }
    
    /* 桌面对话框样式 */
    .desktop-dialog {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 3000;
    }
    
    .dialog-content {
        background: #f8f8f8;
        border: 1px solid #999;
        border-radius: 4px;
        box-shadow: 4px 4px 8px rgba(0,0,0,0.3);
        min-width: 300px;
        max-width: 600px;
    }
    
    .dialog-header {
        background: linear-gradient(to bottom, #4a90e2 0%, #357abd 50%, #1e5f99 51%, #2989d8 100%);
        color: white;
        padding: 6px 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 11px;
    }
    
    .dialog-title {
        font-weight: bold;
    }
    
    .dialog-close {
        background: none;
        border: none;
        color: white;
        cursor: pointer;
        font-size: 14px;
        width: 16px;
        height: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
    }
    
    .dialog-close:hover {
        background: rgba(255,255,255,0.2);
    }
    
    .dialog-body {
        padding: 16px;
        font-size: 11px;
        color: #333;
    }
    
    .dialog-footer {
        padding: 8px 16px;
        border-top: 1px solid #ccc;
        display: flex;
        justify-content: flex-end;
        gap: 8px;
    }
    
    .dialog-footer .btn {
        padding: 4px 12px;
        font-size: 11px;
        border: 1px solid #999;
        background: linear-gradient(to bottom, #f0f0f0 0%, #e0e0e0 100%);
        cursor: pointer;
        border-radius: 2px;
    }
    
    .dialog-footer .btn:hover {
        background: linear-gradient(to bottom, #f8f8f8 0%, #e8e8e8 100%);
    }
    
    .dialog-footer .btn-primary {
        background: linear-gradient(to bottom, #4a90e2 0%, #357abd 100%);
        color: white;
        border-color: #2980b9;
    }
    
    .dialog-footer .btn-primary:hover {
        background: linear-gradient(to bottom, #5ba0f2 0%, #4690cd 100%);
    }
    
    /* 面包屑分隔符 */
    .breadcrumb-separator {
        color: #999;
        margin: 0 4px;
    }
`;

document.head.appendChild(desktopStyles);

// 设置功能
function showSettings() {
    const settingsContent = `
        <div style="padding: 20px;">
            <h3 style="margin-top: 0;">系统设置</h3>
            
            <div style="margin-bottom: 20px;">
                <h4>界面设置</h4>
                <div style="margin: 10px 0;">
                    <label>
                        <input type="checkbox" checked> 显示工具栏标签
                    </label>
                </div>
                <div style="margin: 10px 0;">
                    <label>
                        <input type="checkbox" checked> 显示状态栏
                    </label>
                </div>
                <div style="margin: 10px 0;">
                    <label>
                        <input type="checkbox"> 自动隐藏右侧面板
                    </label>
                </div>
            </div>
            
            <div style="margin-bottom: 20px;">
                <h4>数据设置</h4>
                <div style="margin: 10px 0;">
                    <label>自动刷新间隔：</label>
                    <select style="margin-left: 10px;">
                        <option value="5">5秒</option>
                        <option value="10" selected>10秒</option>
                        <option value="30">30秒</option>
                        <option value="60">1分钟</option>
                    </select>
                </div>
                <div style="margin: 10px 0;">
                    <label>
                        <input type="checkbox" checked> 启用实时数据更新
                    </label>
                </div>
            </div>
            
            <div style="margin-bottom: 20px;">
                <h4>设备设置</h4>
                <div style="margin: 10px 0;">
                    <label>扫码器端口：</label>
                    <input type="text" value="COM1" style="margin-left: 10px; width: 100px;">
                </div>
                <div style="margin: 10px 0;">
                    <label>打印机IP：</label>
                    <input type="text" value="192.168.1.100" style="margin-left: 10px; width: 150px;">
                </div>
            </div>
        </div>
    `;
    
    const buttons = `
        <button class="btn" onclick="closeDialog(this)">取消</button>
        <button class="btn btn-primary" onclick="saveSettings(); closeDialog(this);">保存设置</button>
    `;
    
    showDialog('系统设置', settingsContent, buttons);
}

function saveSettings() {
    showMessage('设置已保存', 'success');
}

// 下拉菜单通用函数
function toggleDropdownMenu(button, dropdownClass) {
    const dropdown = button.parentElement.querySelector(dropdownClass);
    const isVisible = dropdown.classList.contains('show');
    
    // 关闭所有其他下拉菜单
    document.querySelectorAll('.function-dropdown.show, .task-dropdown.show').forEach(menu => {
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

// 功能下拉菜单
function toggleFunctionMenu(button) {
    toggleDropdownMenu(button, '.function-dropdown');
}

// 任务管理下拉菜单
function toggleTaskMenu(button) {
    toggleDropdownMenu(button, '.task-dropdown');
}

// 退出应用程序
function exitApplication() {
    const confirmDialog = `
        <div style="text-align: center; padding: 20px;">
            <div style="font-size: 48px; margin-bottom: 16px;">⚠️</div>
            <h3>确认退出</h3>
            <p>您确定要退出产线采集关联软件吗？</p>
            <p style="color: #666; font-size: 12px;">未保存的数据可能会丢失</p>
        </div>
    `;
    
    const buttons = `
        <button class="btn" onclick="closeDialog(this)">取消</button>
        <button class="btn btn-danger" onclick="confirmExit(); closeDialog(this);">确认退出</button>
    `;
    
    showDialog('退出确认', confirmDialog, buttons);
}

function confirmExit() {
    showMessage('正在退出系统...', 'info');
    
    // 模拟退出过程
    setTimeout(() => {
        showMessage('系统已安全退出', 'success');
        // 在实际应用中，这里会关闭应用程序
        setTimeout(() => {
            window.close();
        }, 1000);
    }, 1000);
}

// 全局函数
window.toggleNode = toggleNode;
window.openPage = openPage;
window.refreshContent = refreshContent;
window.closeDialog = closeDialog;
window.showSettings = showSettings;
window.saveSettings = saveSettings;
window.toggleFunctionMenu = toggleFunctionMenu;
window.toggleTaskMenu = toggleTaskMenu;
window.exitApplication = exitApplication;
window.confirmExit = confirmExit;

// 产品管理功能
let productData = [];
let currentPage = 1;
let pageSize = 20;
let totalProducts = 0;

// 打开产品管理弹框
function openProductManagement() {
    const modal = document.getElementById('productManagementModal');
    if (modal) {
        modal.style.display = 'flex';
        loadProductData();
        initProductManagementEvents();
    }
}

// 初始化产品管理事件
function initProductManagementEvents() {
    // 关闭弹框
    const closeBtn = document.getElementById('closeProductModalBtn');
    if (closeBtn) {
        closeBtn.onclick = () => {
            document.getElementById('productManagementModal').style.display = 'none';
        };
    }

    // 同步产品
    const syncBtn = document.getElementById('syncProductBtn');
    if (syncBtn) {
        syncBtn.onclick = syncProducts;
    }

    // 手动添加产品
    const addBtn = document.getElementById('addProductBtn');
    if (addBtn) {
        addBtn.onclick = openAddProductModal;
    }

    // 搜索功能
    const searchBtn = document.getElementById('searchProductBtn');
    if (searchBtn) {
        searchBtn.onclick = searchProducts;
    }

    // 回车搜索
    const codeSearch = document.getElementById('productCodeSearch');
    const nameSearch = document.getElementById('productNameSearch');
    if (codeSearch) {
        codeSearch.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') searchProducts();
        });
    }
    if (nameSearch) {
        nameSearch.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') searchProducts();
        });
    }
}

// 加载产品数据
function loadProductData() {
    // 模拟产品数据
    productData = [
        {
            id: 1,
            productCode: 'PRD001',
            productName: '阿莫西林胶囊',
            specification: '0.25g*24粒',
            packageRatio: '24',
            minUnit: '粒'
        },
        {
            id: 2,
            productCode: 'PRD002',
            productName: '感冒灵颗粒',
            specification: '10g*12袋',
            packageRatio: '12:48',
            minUnit: '袋'
        },
        {
            id: 3,
            productCode: 'PRD003',
            productName: '维生素C片',
            specification: '100mg*100片',
            packageRatio: '6:12:48',
            minUnit: '片'
        },
        {
            id: 4,
            productCode: 'PRD004',
            productName: '板蓝根颗粒',
            specification: '10g*20袋',
            packageRatio: '20',
            minUnit: '袋'
        },
        {
            id: 5,
            productCode: 'PRD005',
            productName: '复方甘草片',
            specification: '50片/瓶',
            packageRatio: '50:24',
            minUnit: '片'
        }
    ];
    
    totalProducts = productData.length;
    renderProductTable();
    updateProductCount();
}

// 渲染产品表格
function renderProductTable() {
    const tbody = document.getElementById('productListBody');
    if (!tbody) return;

    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const pageData = productData.slice(startIndex, endIndex);

    tbody.innerHTML = '';
    
    pageData.forEach((product, index) => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${startIndex + index + 1}</td>
            <td>${product.productCode}</td>
            <td>${product.productName}</td>
            <td>${product.specification}</td>
            <td>${product.packageRatio}</td>
            <td>${product.minUnit}</td>
            <td>
                <button class="btn btn-secondary action-btn" onclick="editProduct(${product.id})">修改</button>
                <button class="btn btn-danger action-btn" onclick="deleteProduct(${product.id})">删除</button>
            </td>
        `;
        tbody.appendChild(row);
    });

    renderPagination();
}

// 渲染分页
function renderPagination() {
    const container = document.getElementById('productPaginationControls');
    if (!container) return;

    const totalPages = Math.ceil(totalProducts / pageSize);
    
    let html = '';
    
    // 上一页
    html += `<button class="btn pagination-btn" ${currentPage === 1 ? 'disabled' : ''} onclick="changePage(${currentPage - 1})">上一页</button>`;
    
    // 页码
    for (let i = 1; i <= totalPages; i++) {
        if (i === currentPage) {
            html += `<button class="btn pagination-btn active">${i}</button>`;
        } else {
            html += `<button class="btn pagination-btn" onclick="changePage(${i})">${i}</button>`;
        }
    }
    
    // 下一页
    html += `<button class="btn pagination-btn" ${currentPage === totalPages ? 'disabled' : ''} onclick="changePage(${currentPage + 1})">下一页</button>`;
    
    container.innerHTML = html;
}

// 切换页码
function changePage(page) {
    if (page < 1 || page > Math.ceil(totalProducts / pageSize)) return;
    currentPage = page;
    renderProductTable();
}

// 更新产品数量显示
function updateProductCount() {
    const countElement = document.getElementById('productTotalCount');
    if (countElement) {
        countElement.textContent = totalProducts;
    }
}

// 搜索产品
function searchProducts() {
    const codeSearch = document.getElementById('productCodeSearch').value.trim();
    const nameSearch = document.getElementById('productNameSearch').value.trim();
    
    // 重新加载原始数据
    loadProductData();
    
    // 过滤数据
    if (codeSearch || nameSearch) {
        productData = productData.filter(product => {
            const codeMatch = !codeSearch || product.productCode.toLowerCase().includes(codeSearch.toLowerCase());
            const nameMatch = !nameSearch || product.productName.toLowerCase().includes(nameSearch.toLowerCase());
            return codeMatch && nameMatch;
        });
        totalProducts = productData.length;
    }
    
    currentPage = 1;
    renderProductTable();
    updateProductCount();
}

// 同步产品
function syncProducts() {
    showMessage('正在同步产品数据...', 'info');
    
    // 模拟同步过程
    setTimeout(() => {
        loadProductData();
        showMessage('产品数据同步完成', 'success');
    }, 1500);
}

// 打开添加产品弹框
function openAddProductModal() {
    const modal = document.getElementById('addProductModal');
    if (modal) {
        modal.style.display = 'flex';
        initAddProductEvents();
        clearAddProductForm();
    }
}

// 初始化添加产品事件
function initAddProductEvents() {
    // 关闭弹框
    const closeBtn = document.getElementById('closeAddProductModalBtn');
    const cancelBtn = document.getElementById('cancelAddProductBtn');
    
    if (closeBtn) {
        closeBtn.onclick = () => {
            document.getElementById('addProductModal').style.display = 'none';
        };
    }
    
    if (cancelBtn) {
        cancelBtn.onclick = () => {
            document.getElementById('addProductModal').style.display = 'none';
        };
    }

    // 保存产品
    const saveBtn = document.getElementById('saveNewProductBtn');
    if (saveBtn) {
        saveBtn.onclick = saveNewProduct;
    }
}

// 清空添加产品表单
function clearAddProductForm() {
    const form = document.getElementById('addProductForm');
    if (form) {
        form.reset();
    }
}

// 保存新产品
function saveNewProduct() {
    const form = document.getElementById('addProductForm');
    if (!form) return;

    const formData = new FormData(form);
    const newProductData = {
        productCode: formData.get('productCode'),
        productName: formData.get('productName'),
        specification: formData.get('specification'),
        packageRatio: formData.get('packageRatio'),
        minUnit: formData.get('minUnit')
    };

    // 验证必填字段
    if (!newProductData.productCode || !newProductData.productName || !newProductData.packageRatio || !newProductData.minUnit) {
        showMessage('请填写所有必填字段', 'error');
        return;
    }

    // 检查产品编号是否重复
    const existingProduct = productData.find(p => p.productCode === newProductData.productCode);
    if (existingProduct) {
        showMessage('产品编号已存在', 'error');
        return;
    }

    // 添加产品
    const newProduct = {
        id: Date.now(),
        ...newProductData
    };

    // 模拟保存过程
    showMessage('正在保存产品...', 'info');
    
    setTimeout(() => {
        // 添加到产品列表
        productData.push(newProduct);
        totalProducts = productData.length;
        
        // 关闭弹框
        document.getElementById('addProductModal').style.display = 'none';
        
        // 刷新产品列表
        renderProductTable();
        updateProductCount();
        
        showMessage('产品添加成功', 'success');
    }, 1000);
}

// 编辑产品
function editProduct(productId) {
    const product = productData.find(p => p.id === productId);
    if (!product) return;

    const modal = document.getElementById('editProductModal');
    if (modal) {
        modal.style.display = 'flex';
        initEditProductEvents();
        fillEditProductForm(product);
    }
}

// 初始化编辑产品事件
function initEditProductEvents() {
    // 关闭弹框
    const closeBtn = document.getElementById('closeEditProductModalBtn');
    const cancelBtn = document.getElementById('cancelEditProductBtn');
    
    if (closeBtn) {
        closeBtn.onclick = () => {
            document.getElementById('editProductModal').style.display = 'none';
        };
    }
    
    if (cancelBtn) {
        cancelBtn.onclick = () => {
            document.getElementById('editProductModal').style.display = 'none';
        };
    }

    // 更新产品
    const updateBtn = document.getElementById('updateProductBtn');
    if (updateBtn) {
        updateBtn.onclick = updateProduct;
    }
}

// 填充编辑产品表单
function fillEditProductForm(product) {
    const form = document.getElementById('editProductForm');
    if (!form) return;

    form.querySelector('[name="productId"]').value = product.id;
    form.querySelector('[name="productCode"]').value = product.productCode;
    form.querySelector('[name="productName"]').value = product.productName;
    form.querySelector('[name="specification"]').value = product.specification || '';
    form.querySelector('[name="packageRatio"]').value = product.packageRatio;
    form.querySelector('[name="minUnit"]').value = product.minUnit;
}

// 更新产品
function updateProduct() {
    const form = document.getElementById('editProductForm');
    if (!form) return;

    const formData = new FormData(form);
    const productId = parseInt(formData.get('productId'));
    const updatedData = {
        productCode: formData.get('productCode'),
        productName: formData.get('productName'),
        specification: formData.get('specification'),
        packageRatio: formData.get('packageRatio'),
        minUnit: formData.get('minUnit')
    };

    // 验证必填字段
    if (!updatedData.productCode || !updatedData.productName || !updatedData.packageRatio || !updatedData.minUnit) {
        showMessage('请填写所有必填字段', 'error');
        return;
    }

    // 检查产品编号是否重复（排除自己）
    const existingProduct = productData.find(p => p.productCode === updatedData.productCode && p.id !== productId);
    if (existingProduct) {
        showMessage('产品编号已存在', 'error');
        return;
    }

    // 模拟更新过程
    showMessage('正在更新产品...', 'info');
    
    setTimeout(() => {
        // 更新产品数据
        const productIndex = productData.findIndex(p => p.id === productId);
        if (productIndex !== -1) {
            productData[productIndex] = { id: productId, ...updatedData };
        }
        
        // 关闭弹框
        document.getElementById('editProductModal').style.display = 'none';
        
        // 刷新产品列表
        renderProductTable();
        
        showMessage('产品更新成功', 'success');
    }, 1000);
}

// 删除产品
function deleteProduct(productId) {
    const product = productData.find(p => p.id === productId);
    if (!product) return;

    if (confirm(`确定要删除产品 "${product.productName}" 吗？`)) {
        showMessage('正在删除产品...', 'info');
        
        setTimeout(() => {
            // 从数据中删除
            const index = productData.findIndex(p => p.id === productId);
            if (index !== -1) {
                productData.splice(index, 1);
                totalProducts = productData.length;
            }
            
            // 如果当前页没有数据了，回到上一页
            if (productData.length > 0 && ((currentPage - 1) * pageSize) >= productData.length) {
                currentPage = Math.max(1, currentPage - 1);
            }
            
            // 刷新列表
            renderProductTable();
            updateProductCount();
            
            showMessage('产品删除成功', 'success');
        }, 500);
    }
}

// 导出全局函数
window.openProductManagement = openProductManagement;
window.changePage = changePage;
window.editProduct = editProduct;
window.deleteProduct = deleteProduct;

// 确保函数立即可用
if (typeof window !== 'undefined') {
    window.openProductManagement = openProductManagement;
} 