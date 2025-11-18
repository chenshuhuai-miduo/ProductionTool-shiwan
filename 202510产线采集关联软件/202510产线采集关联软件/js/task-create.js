// 任务创建页面JavaScript功能

// 全局变量
let currentStep = 1;
let totalSteps = 3;
let formData = {
    basic: {},
    product: {},
    confirm: {}
};
let selectedProduct = null;

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
    
    // 初始化步骤
    initSteps();
    
    // 显示第一步
    showStep(1);
    
    // 显示欢迎消息
    setTimeout(() => {
        showMessage('任务创建向导已启动', 'info');
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
    // 步骤按钮
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const saveDraftBtn = document.getElementById('saveDraftBtn');
    const createBtn = document.getElementById('createBtn');
    const createAndExecuteBtn = document.getElementById('createAndExecuteBtn');
    
    if (prevBtn) prevBtn.addEventListener('click', handlePrevStep);
    if (nextBtn) nextBtn.addEventListener('click', handleNextStep);
    if (saveDraftBtn) saveDraftBtn.addEventListener('click', handleSaveDraft);
    if (createBtn) createBtn.addEventListener('click', handleCreateTask);
    if (createAndExecuteBtn) createAndExecuteBtn.addEventListener('click', handleCreateAndExecute);
    
    // 产品选择
    const productSelect = document.getElementById('productSelect');
    if (productSelect) {
        productSelect.addEventListener('change', handleProductSelect);
    }
    
    // 表单验证
    initFormValidation();
}

function initSteps() {
    // 初始化步骤指示器
    updateStepIndicator();
}

function showStep(step) {
    // 隐藏所有步骤内容
    const stepContents = document.querySelectorAll('.step-content');
    stepContents.forEach(content => {
        content.style.display = 'none';
    });
    
    // 显示当前步骤内容
    const currentContent = document.getElementById(`step${step}Content`);
    if (currentContent) {
        currentContent.style.display = 'block';
    }
    
    // 更新步骤指示器
    currentStep = step;
    updateStepIndicator();
    
    // 更新按钮状态
    updateButtonStates();
    
    // 根据步骤执行特定逻辑
    switch(step) {
        case 1:
            initBasicInfoStep();
            break;
        case 2:
            initProductConfigStep();
            break;
        case 3:
            initConfirmStep();
            break;
    }
}

function updateStepIndicator() {
    const steps = document.querySelectorAll('.step');
    steps.forEach((step, index) => {
        const stepNumber = index + 1;
        step.classList.remove('active', 'completed');
        
        if (stepNumber < currentStep) {
            step.classList.add('completed');
        } else if (stepNumber === currentStep) {
            step.classList.add('active');
        }
    });
}

function updateButtonStates() {
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const createBtn = document.getElementById('createBtn');
    const createAndExecuteBtn = document.getElementById('createAndExecuteBtn');
    
    // 上一步按钮
    if (prevBtn) {
        prevBtn.style.display = currentStep > 1 ? 'inline-block' : 'none';
    }
    
    // 下一步按钮
    if (nextBtn) {
        nextBtn.style.display = currentStep < totalSteps ? 'inline-block' : 'none';
    }
    
    // 创建按钮
    if (createBtn) {
        createBtn.style.display = currentStep === totalSteps ? 'inline-block' : 'none';
    }
    
    if (createAndExecuteBtn) {
        createAndExecuteBtn.style.display = currentStep === totalSteps ? 'inline-block' : 'none';
    }
}

// 步骤处理函数
function handlePrevStep() {
    if (currentStep > 1) {
        showStep(currentStep - 1);
    }
}

function handleNextStep() {
    if (validateCurrentStep()) {
        saveCurrentStepData();
        if (currentStep < totalSteps) {
            showStep(currentStep + 1);
        }
    }
}

function validateCurrentStep() {
    switch(currentStep) {
        case 1:
            return validateBasicInfo();
        case 2:
            return validateProductConfig();
        case 3:
            return true; // 确认步骤不需要验证
        default:
            return true;
    }
}

function saveCurrentStepData() {
    switch(currentStep) {
        case 1:
            saveBasicInfoData();
            break;
        case 2:
            saveProductConfigData();
            break;
    }
}

// 第一步：基本信息
function initBasicInfoStep() {
    // 生成任务编号
    generateTaskCode();
    
    // 设置默认值
    const plannedStartDate = document.getElementById('plannedStartDate');
    if (plannedStartDate && !plannedStartDate.value) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        plannedStartDate.value = tomorrow.toISOString().split('T')[0];
    }
    
    const plannedEndDate = document.getElementById('plannedEndDate');
    if (plannedEndDate && !plannedEndDate.value) {
        const nextWeek = new Date();
        nextWeek.setDate(nextWeek.getDate() + 7);
        plannedEndDate.value = nextWeek.toISOString().split('T')[0];
    }
}

function generateTaskCode() {
    const taskCodeInput = document.getElementById('taskCode');
    if (taskCodeInput && !taskCodeInput.value) {
        const now = new Date();
        const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
        const timeStr = now.toTimeString().slice(0, 8).replace(/:/g, '');
        const randomStr = Math.random().toString(36).substr(2, 4).toUpperCase();
        taskCodeInput.value = `T${dateStr}${timeStr}${randomStr}`;
    }
}

function validateBasicInfo() {
    const form = document.getElementById('basicInfoForm');
    if (!form) return false;
    
    const validator = new Validator(form);
    return validator.validate();
}

function saveBasicInfoData() {
    const form = document.getElementById('basicInfoForm');
    if (!form) return;
    
    const formData = new FormData(form);
    for (let [key, value] of formData.entries()) {
        formData.basic[key] = value;
    }
}

// 第二步：产品配置
function initProductConfigStep() {
    // 加载产品列表
    loadProductList();
    
    // 如果已选择产品，显示产品信息
    if (selectedProduct) {
        showProductInfo(selectedProduct);
    }
}

function loadProductList() {
    const productSelect = document.getElementById('productSelect');
    if (!productSelect) return;
    
    // 模拟产品数据
    const products = [
        { id: 1, code: 'P001', name: '维生素C片', specification: '100mg*100片', packageRatio: '24', unit: '瓶' },
        { id: 2, code: 'P002', name: '感冒灵颗粒', specification: '10g*12袋', packageRatio: '12:48', unit: '盒' },
        { id: 3, code: 'P003', name: '阿莫西林胶囊', specification: '250mg*24粒', packageRatio: '6:12:48', unit: '盒' },
        { id: 4, code: 'P004', name: '板蓝根颗粒', specification: '10g*20袋', packageRatio: '20', unit: '盒' },
        { id: 5, code: 'P005', name: '复合维生素片', specification: '60片', packageRatio: '10:30', unit: '瓶' }
    ];
    
    // 清空现有选项
    productSelect.innerHTML = '<option value="">请选择产品...</option>';
    
    // 添加产品选项
    products.forEach(product => {
        const option = document.createElement('option');
        option.value = product.id;
        option.textContent = `${product.code} - ${product.name} (${product.specification})`;
        option.dataset.product = JSON.stringify(product);
        productSelect.appendChild(option);
    });
}

function handleProductSelect(event) {
    const selectedOption = event.target.selectedOptions[0];
    if (selectedOption && selectedOption.dataset.product) {
        selectedProduct = JSON.parse(selectedOption.dataset.product);
        showProductInfo(selectedProduct);
        showAdaptMode(selectedProduct.packageRatio);
        calculateAutoValues(selectedProduct);
    } else {
        selectedProduct = null;
        hideProductInfo();
        hideAdaptMode();
        clearAutoValues();
    }
}

function showProductInfo(product) {
    const productCard = document.getElementById('productInfoCard');
    if (!productCard) return;
    
    productCard.style.display = 'block';
    
    // 更新产品信息
    const codeSpan = productCard.querySelector('.product-code');
    const nameSpan = productCard.querySelector('.product-name');
    const specSpan = productCard.querySelector('.product-spec');
    const unitSpan = productCard.querySelector('.product-unit');
    const ratioSpan = productCard.querySelector('.product-ratio');
    
    if (codeSpan) codeSpan.textContent = product.code;
    if (nameSpan) nameSpan.textContent = product.name;
    if (specSpan) specSpan.textContent = product.specification;
    if (unitSpan) unitSpan.textContent = product.unit;
    if (ratioSpan) ratioSpan.textContent = product.packageRatio;
}

function hideProductInfo() {
    const productCard = document.getElementById('productInfoCard');
    if (productCard) {
        productCard.style.display = 'none';
    }
}

function showAdaptMode(packageRatio) {
    const adaptCard = document.getElementById('adaptModeCard');
    if (!adaptCard) return;
    
    adaptCard.style.display = 'block';
    
    // 解析包装比例并显示适配模式
    const modeInfo = PackageUtils.parsePackageRatio(packageRatio);
    const modeSpan = adaptCard.querySelector('.adapt-mode');
    const descSpan = adaptCard.querySelector('.adapt-desc');
    
    if (modeSpan) modeSpan.textContent = modeInfo.mode;
    if (descSpan) descSpan.textContent = modeInfo.description;
    
    // 设置颜色
    const modeTag = adaptCard.querySelector('.package-tag');
    if (modeTag) {
        modeTag.className = `package-tag ${modeInfo.level}`;
    }
}

function hideAdaptMode() {
    const adaptCard = document.getElementById('adaptModeCard');
    if (adaptCard) {
        adaptCard.style.display = 'none';
    }
}

function calculateAutoValues(product) {
    const autoCard = document.getElementById('autoCalcCard');
    if (!autoCard) return;
    
    autoCard.style.display = 'block';
    
    // 获取计划数量
    const plannedQtyInput = document.getElementById('plannedQty');
    const plannedQty = parseInt(plannedQtyInput?.value) || 1000;
    
    // 解析包装比例
    const ratios = product.packageRatio.split(':').map(r => parseInt(r));
    
    let calculations = {};
    
    if (ratios.length === 1) {
        // 二级关联
        calculations.pallets = Math.ceil(plannedQty / ratios[0]);
        calculations.boxes = 0;
        calculations.pieces = plannedQty;
    } else if (ratios.length === 2) {
        // 三级关联
        const piecesPerBox = ratios[0];
        const boxesPerPallet = ratios[1];
        calculations.boxes = Math.ceil(plannedQty / piecesPerBox);
        calculations.pallets = Math.ceil(calculations.boxes / boxesPerPallet);
        calculations.pieces = plannedQty;
    } else if (ratios.length === 3) {
        // 四级关联
        const piecesPerSmallBox = ratios[0];
        const smallBoxesPerBox = ratios[1];
        const boxesPerPallet = ratios[2];
        const piecesPerBox = piecesPerSmallBox * smallBoxesPerBox;
        calculations.boxes = Math.ceil(plannedQty / piecesPerBox);
        calculations.pallets = Math.ceil(calculations.boxes / boxesPerPallet);
        calculations.pieces = plannedQty;
    }
    
    // 估算完成时间（假设每小时处理100个单品）
    const hoursNeeded = Math.ceil(plannedQty / 100);
    const completionTime = new Date();
    completionTime.setHours(completionTime.getHours() + hoursNeeded);
    
    // 更新显示
    updateAutoCalcDisplay(calculations, completionTime);
}

function updateAutoCalcDisplay(calculations, completionTime) {
    const autoCard = document.getElementById('autoCalcCard');
    if (!autoCard) return;
    
    const palletsSpan = autoCard.querySelector('.calc-pallets');
    const boxesSpan = autoCard.querySelector('.calc-boxes');
    const piecesSpan = autoCard.querySelector('.calc-pieces');
    const timeSpan = autoCard.querySelector('.calc-time');
    const devicesSpan = autoCard.querySelector('.calc-devices');
    
    if (palletsSpan) palletsSpan.textContent = calculations.pallets || 0;
    if (boxesSpan) boxesSpan.textContent = calculations.boxes || 0;
    if (piecesSpan) piecesSpan.textContent = calculations.pieces || 0;
    if (timeSpan) timeSpan.textContent = completionTime.toLocaleString('zh-CN');
    if (devicesSpan) devicesSpan.textContent = Math.max(1, Math.ceil(calculations.pallets / 10)); // 假设每台设备处理10垛
}

function clearAutoValues() {
    const autoCard = document.getElementById('autoCalcCard');
    if (autoCard) {
        autoCard.style.display = 'none';
    }
}

function validateProductConfig() {
    if (!selectedProduct) {
        showMessage('请选择产品', 'warning');
        return false;
    }
    
    const plannedQtyInput = document.getElementById('plannedQty');
    const plannedQty = parseInt(plannedQtyInput?.value);
    
    if (!plannedQty || plannedQty <= 0) {
        showMessage('请输入有效的计划数量', 'warning');
        return false;
    }
    
    return true;
}

function saveProductConfigData() {
    formData.product = {
        productId: selectedProduct?.id,
        productCode: selectedProduct?.code,
        productName: selectedProduct?.name,
        packageRatio: selectedProduct?.packageRatio,
        plannedQty: document.getElementById('plannedQty')?.value,
        priority: document.getElementById('priority')?.value,
        remarks: document.getElementById('remarks')?.value
    };
}

// 第三步：确认创建
function initConfirmStep() {
    // 显示汇总信息
    showTaskSummary();
    
    // 显示检查清单
    showCheckList();
}

function showTaskSummary() {
    const summaryContainer = document.getElementById('taskSummary');
    if (!summaryContainer) return;
    
    const basicInfo = formData.basic;
    const productInfo = formData.product;
    
    const summaryHTML = `
        <div class="summary-section">
            <h4>基本信息</h4>
            <div class="summary-item">
                <span class="label">任务编号：</span>
                <span class="value">${basicInfo.taskCode || ''}</span>
            </div>
            <div class="summary-item">
                <span class="label">任务名称：</span>
                <span class="value">${basicInfo.taskName || ''}</span>
            </div>
            <div class="summary-item">
                <span class="label">计划开始：</span>
                <span class="value">${basicInfo.plannedStartDate || ''}</span>
            </div>
            <div class="summary-item">
                <span class="label">计划结束：</span>
                <span class="value">${basicInfo.plannedEndDate || ''}</span>
            </div>
        </div>
        
        <div class="summary-section">
            <h4>产品配置</h4>
            <div class="summary-item">
                <span class="label">产品编号：</span>
                <span class="value">${productInfo.productCode || ''}</span>
            </div>
            <div class="summary-item">
                <span class="label">产品名称：</span>
                <span class="value">${productInfo.productName || ''}</span>
            </div>
            <div class="summary-item">
                <span class="label">包装比例：</span>
                <span class="value">${productInfo.packageRatio || ''}</span>
            </div>
            <div class="summary-item">
                <span class="label">计划数量：</span>
                <span class="value">${productInfo.plannedQty || ''}</span>
            </div>
            <div class="summary-item">
                <span class="label">优先级：</span>
                <span class="value">${productInfo.priority || ''}</span>
            </div>
        </div>
    `;
    
    summaryContainer.innerHTML = summaryHTML;
}

function showCheckList() {
    const checkListContainer = document.getElementById('checkList');
    if (!checkListContainer) return;
    
    const checkItems = [
        { id: 'check1', text: '确认产品信息正确', checked: true },
        { id: 'check2', text: '确认包装比例配置正确', checked: true },
        { id: 'check3', text: '确认计划数量合理', checked: true },
        { id: 'check4', text: '确认时间安排合适', checked: false },
        { id: 'check5', text: '确认设备资源可用', checked: false }
    ];
    
    const checkListHTML = checkItems.map(item => `
        <div class="check-item">
            <input type="checkbox" id="${item.id}" ${item.checked ? 'checked' : ''}>
            <label for="${item.id}">${item.text}</label>
        </div>
    `).join('');
    
    checkListContainer.innerHTML = checkListHTML;
}

// 操作处理函数
function handleSaveDraft() {
    saveCurrentStepData();
    
    const draftData = {
        ...formData,
        status: 'draft',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
    };
    
    // 模拟保存草稿
    localStorage.setItem('taskDraft', JSON.stringify(draftData));
    
    showMessage('草稿已保存', 'success');
}

function handleCreateTask() {
    if (validateFinalStep()) {
        createTask(false);
    }
}

function handleCreateAndExecute() {
    if (validateFinalStep()) {
        createTask(true);
    }
}

function validateFinalStep() {
    // 检查必要的检查项
    const requiredChecks = ['check1', 'check2', 'check3'];
    for (let checkId of requiredChecks) {
        const checkbox = document.getElementById(checkId);
        if (!checkbox || !checkbox.checked) {
            showMessage('请完成所有必要的检查项', 'warning');
            return false;
        }
    }
    
    return true;
}

function createTask(executeImmediately = false) {
    // 显示创建进度
    showCreatingProgress();
    
    // 模拟任务创建过程
    setTimeout(() => {
        const taskData = {
            ...formData,
            id: Utils.generateUUID(),
            status: executeImmediately ? 'running' : 'pending',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        };
        
        // 保存任务数据
        const existingTasks = JSON.parse(localStorage.getItem('tasks') || '[]');
        existingTasks.push(taskData);
        localStorage.setItem('tasks', JSON.stringify(existingTasks));
        
        // 清除草稿
        localStorage.removeItem('taskDraft');
        
        hideCreatingProgress();
        
        if (executeImmediately) {
            showMessage('任务创建成功并已开始执行', 'success');
            setTimeout(() => {
                window.location.href = 'task-control.html';
            }, 2000);
        } else {
            showMessage('任务创建成功', 'success');
            setTimeout(() => {
                window.location.href = 'task-list.html';
            }, 2000);
        }
    }, 2000);
}

function showCreatingProgress() {
    const progressHTML = `
        <div class="creating-progress">
            <div class="progress-icon">⏳</div>
            <div class="progress-text">正在创建任务...</div>
            <div class="progress-bar">
                <div class="progress-fill"></div>
            </div>
        </div>
    `;
    
    const progressContainer = document.createElement('div');
    progressContainer.className = 'progress-overlay';
    progressContainer.innerHTML = progressHTML;
    document.body.appendChild(progressContainer);
    
    // 模拟进度
    let progress = 0;
    const progressFill = progressContainer.querySelector('.progress-fill');
    const progressText = progressContainer.querySelector('.progress-text');
    
    const progressInterval = setInterval(() => {
        progress += 10;
        if (progressFill) {
            progressFill.style.width = `${progress}%`;
        }
        
        if (progress >= 100) {
            clearInterval(progressInterval);
            if (progressText) {
                progressText.textContent = '任务创建完成';
            }
        }
    }, 200);
}

function hideCreatingProgress() {
    const progressOverlay = document.querySelector('.progress-overlay');
    if (progressOverlay) {
        document.body.removeChild(progressOverlay);
    }
}

// 表单验证初始化
function initFormValidation() {
    // 计划数量输入监听
    const plannedQtyInput = document.getElementById('plannedQty');
    if (plannedQtyInput) {
        plannedQtyInput.addEventListener('input', function() {
            if (selectedProduct) {
                calculateAutoValues(selectedProduct);
            }
        });
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

// 添加样式
const taskCreateStyles = document.createElement('style');
taskCreateStyles.textContent = `
    /* 创建进度样式 */
    .progress-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
    }
    
    .creating-progress {
        background: white;
        padding: 32px;
        border-radius: 8px;
        text-align: center;
        min-width: 300px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
    }
    
    .progress-icon {
        font-size: 48px;
        margin-bottom: 16px;
    }
    
    .progress-text {
        font-size: 16px;
        color: #333;
        margin-bottom: 20px;
    }
    
    .progress-bar {
        width: 100%;
        height: 8px;
        background: #f0f0f0;
        border-radius: 4px;
        overflow: hidden;
    }
    
    .progress-fill {
        height: 100%;
        background: #1890ff;
        width: 0%;
        transition: width 0.3s ease;
    }
    
    /* 汇总信息样式 */
    .summary-section {
        margin-bottom: 24px;
        padding: 16px;
        background: #f8f9fa;
        border-radius: 6px;
        border-left: 4px solid #1890ff;
    }
    
    .summary-section h4 {
        margin: 0 0 12px 0;
        color: #333;
        font-size: 16px;
    }
    
    .summary-item {
        display: flex;
        margin-bottom: 8px;
        font-size: 14px;
    }
    
    .summary-item .label {
        width: 100px;
        color: #666;
        flex-shrink: 0;
    }
    
    .summary-item .value {
        color: #333;
        font-weight: 500;
    }
    
    /* 检查清单样式 */
    .check-item {
        display: flex;
        align-items: center;
        padding: 8px 0;
        border-bottom: 1px solid #f0f0f0;
    }
    
    .check-item:last-child {
        border-bottom: none;
    }
    
    .check-item input[type="checkbox"] {
        margin-right: 8px;
        transform: scale(1.2);
    }
    
    .check-item label {
        color: #333;
        cursor: pointer;
        font-size: 14px;
    }
`;

document.head.appendChild(taskCreateStyles); 