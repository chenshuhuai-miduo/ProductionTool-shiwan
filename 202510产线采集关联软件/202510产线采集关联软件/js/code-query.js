/**
 * 查询码功能界面 JavaScript
 * 实现码查询、关联展示、历史记录、状态管理等交互功能
 */

// 全局变量
let queryHistory = [];
let queryStats = {
    todayCount: 0,
    totalCount: 0,
    successCount: 0
};
let currentQueryCode = '';
let isQuerying = false;

// 模拟数据
const mockData = {
    // 模拟产品数据
    products: {
        'PRD002': {
            productCode: 'PRD002',
            productName: '保密产品A',
            productSpec: '标准规格',
            measureUnit: '个',
            packageLevel: '第一层'
        }
    },
    
    // 模拟批次数据
    batches: {
        'B20241202001': {
            batchNumber: 'B20241202001',
            productionDate: '2024-12-02',
            productionShift: '早班',
            productionLine: '生产线002',
            qualityStatus: '合格',
            codeStatus: '正常'
        }
    },
    
    // 模拟码关联数据
    codeRelations: {
        'L1_002_0001': {
            level: 1,
            code: 'L1_002_0001',
            collectTime: '2024/12/02 10:30:17.448',
            productCode: 'PRD002',
            batchNumber: 'B20241202001',
            relations: [
                {
                    level1: { code: 'L1_002_0001', time: '2024/12/02 10:30:17.448' },
                    level2: { code: 'L2_002_0001', time: '2024/12/02 10:30:18.921' },
                    level3: { code: 'L3_002_0001', time: '2024/12/02 10:30:19.988' },
                    level4: { code: 'L4_002_0001', time: '2024/12/02 10:30:20.279' }
                },
                {
                    level1: { code: 'L1_002_0002', time: '2024/12/02 10:30:19.123' },
                    level2: { code: 'L2_002_0001', time: '2024/12/02 10:30:20.456' },
                    level3: { code: 'L3_002_0001', time: '2024/12/02 10:30:21.789' },
                    level4: { code: 'L4_002_0001', time: '2024/12/02 10:30:22.012' }
                },
                {
                    level1: { code: 'L1_002_0003', time: '2024/12/02 10:30:21.345' },
                    level2: { code: 'L2_002_0001', time: '2024/12/02 10:30:22.678' },
                    level3: { code: 'L3_002_0001', time: '2024/12/02 10:30:23.901' },
                    level4: { code: 'L4_002_0001', time: '2024/12/02 10:30:24.234' }
                }
            ]
        },
        'L2_002_0001': {
            level: 2,
            code: 'L2_002_0001',
            collectTime: '2024/12/02 10:30:18.921',
            productCode: 'PRD002',
            batchNumber: 'B20241202001',
            relations: [
                {
                    level1: { code: 'L1_002_0001', time: '2024/12/02 10:30:17.448' },
                    level2: { code: 'L2_002_0001', time: '2024/12/02 10:30:18.921' },
                    level3: { code: 'L3_002_0001', time: '2024/12/02 10:30:19.988' },
                    level4: { code: 'L4_002_0001', time: '2024/12/02 10:30:20.279' }
                }
            ]
        },
        'L3_002_0001': {
            level: 3,
            code: 'L3_002_0001',
            collectTime: '2024/12/02 10:30:19.988',
            productCode: 'PRD002',
            batchNumber: 'B20241202001',
            relations: [
                {
                    level1: { code: 'L1_002_0001', time: '2024/12/02 10:30:17.448' },
                    level2: { code: 'L2_002_0001', time: '2024/12/02 10:30:18.921' },
                    level3: { code: 'L3_002_0001', time: '2024/12/02 10:30:19.988' },
                    level4: { code: 'L4_002_0001', time: '2024/12/02 10:30:20.279' }
                }
            ]
        },
        'L4_002_0001': {
            level: 4,
            code: 'L4_002_0001',
            collectTime: '2024/12/02 10:30:20.279',
            productCode: 'PRD002',
            batchNumber: 'B20241202001',
            relations: [
                {
                    level1: { code: 'L1_002_0001', time: '2024/12/02 10:30:17.448' },
                    level2: { code: 'L2_002_0001', time: '2024/12/02 10:30:18.921' },
                    level3: { code: 'L3_002_0001', time: '2024/12/02 10:30:19.988' },
                    level4: { code: 'L4_002_0001', time: '2024/12/02 10:30:20.279' }
                }
            ]
        }
    }
};

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    initializePage();
    setupEventListeners();
    startTimeUpdate();
    loadQueryHistory();
    loadQueryStats();
    
    // 检查URL参数是否有预设查询码
    checkUrlParams();
});

// 初始化页面
function initializePage() {
    console.log('查询码功能界面初始化');
    
    // 设置输入框焦点
    const codeInput = document.getElementById('codeInput');
    if (codeInput) {
        codeInput.focus();
    }
    
    // 初始化状态
    updateQueryStatus('请输入码进行查询', 'default');
    updateRelationStats('未查询');
}

// 设置事件监听器
function setupEventListeners() {
    const codeInput = document.getElementById('codeInput');
    const queryBtn = document.getElementById('queryBtn');
    const clearBtn = document.getElementById('clearBtn');
    const scanBtn = document.getElementById('scanBtn');
    
    // 输入框事件
    if (codeInput) {
        codeInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                performQuery();
            }
        });
        
        codeInput.addEventListener('input', function(e) {
            const value = e.target.value.trim();
            if (value) {
                updateQueryStatus('按回车键或点击查询按钮进行查询', 'default');
            } else {
                updateQueryStatus('请输入码进行查询', 'default');
            }
        });
    }
    
    // 按钮事件
    if (queryBtn) {
        queryBtn.addEventListener('click', performQuery);
    }
    
    if (clearBtn) {
        clearBtn.addEventListener('click', clearQuery);
    }
    
    if (scanBtn) {
        scanBtn.addEventListener('click', startScan);
    }
}

// 执行查询
async function performQuery() {
    if (isQuerying) {
        return;
    }
    
    const codeInput = document.getElementById('codeInput');
    const code = codeInput.value.trim();
    
    if (!code) {
        Message.warning('请输入要查询的码');
        codeInput.focus();
        return;
    }
    
    // 验证码格式
    if (!validateCodeFormat(code)) {
        Message.error('码格式不正确，请检查输入');
        return;
    }
    
    isQuerying = true;
    currentQueryCode = code;
    
    try {
        // 显示查询状态
        updateQueryStatus('正在查询中...', 'loading');
        setButtonsDisabled(true);
        
        // 模拟查询延迟
        await new Promise(resolve => setTimeout(resolve, 800));
        
        // 执行查询
        const result = await queryCodeData(code);
        
        if (result.success) {
            // 查询成功
            updateQueryStatus(`查询成功 - ${code}`, 'success');
            displayQueryResult(result.data);
            addToHistory(code, true);
            updateQueryStats(true);
            Message.success('查询成功');
        } else {
            // 查询失败
            updateQueryStatus(`未找到该码信息 - ${code}`, 'error');
            displayNoResult();
            addToHistory(code, false);
            updateQueryStats(false);
            Message.error(result.message || '未找到该码信息');
        }
        
    } catch (error) {
        console.error('查询错误:', error);
        updateQueryStatus('查询服务异常', 'error');
        displayError(error.message);
        addToHistory(code, false);
        updateQueryStats(false);
        Message.error('查询服务异常，请稍后重试');
    } finally {
        isQuerying = false;
        setButtonsDisabled(false);
    }
}

// 查询码数据
async function queryCodeData(code) {
    try {
        // 模拟API调用
        const codeData = mockData.codeRelations[code];
        
        if (codeData) {
            const productData = mockData.products[codeData.productCode];
            const batchData = mockData.batches[codeData.batchNumber];
            
            return {
                success: true,
                data: {
                    code: codeData,
                    product: productData,
                    batch: batchData
                }
            };
        } else {
            return {
                success: false,
                message: '未找到该码信息'
            };
        }
    } catch (error) {
        throw new Error('查询服务异常');
    }
}

// 显示查询结果
function displayQueryResult(data) {
    displayCodeRelations(data.code);
    displayDetailInfo(data.product, data.batch);
    updateRelationStats(`找到 ${data.code.relations.length} 条关联记录`);
}

// 显示码关联信息
function displayCodeRelations(codeData) {
    const relationContent = document.getElementById('relationContent');
    
    if (!codeData.relations || codeData.relations.length === 0) {
        relationContent.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📭</div>
                <p class="empty-text">该码暂无关联信息</p>
            </div>
        `;
        return;
    }
    
    let tableHTML = `
        <table class="relation-table">
            <thead>
                <tr>
                    <th>第一层</th>
                    <th>采集时间</th>
                    <th>第二层</th>
                    <th>采集时间</th>
                    <th>第三层</th>
                    <th>采集时间</th>
                    <th>第四层</th>
                    <th>采集时间</th>
                </tr>
            </thead>
            <tbody>
    `;
    
    codeData.relations.forEach((relation, index) => {
        const isCurrentQuery = relation.level1.code === currentQueryCode || 
                              relation.level2.code === currentQueryCode || 
                              relation.level3.code === currentQueryCode || 
                              relation.level4.code === currentQueryCode;
        
        tableHTML += `
            <tr ${isCurrentQuery ? 'class="current-query"' : ''}>
                <td>
                    <span class="level-code level-1" onclick="queryRelatedCode('${relation.level1.code}')">
                        ${relation.level1.code}
                    </span>
                </td>
                <td><span class="collect-time">${relation.level1.time}</span></td>
                <td>
                    <span class="level-code level-2" onclick="queryRelatedCode('${relation.level2.code}')">
                        ${relation.level2.code}
                    </span>
                </td>
                <td><span class="collect-time">${relation.level2.time}</span></td>
                <td>
                    <span class="level-code level-3" onclick="queryRelatedCode('${relation.level3.code}')">
                        ${relation.level3.code}
                    </span>
                </td>
                <td><span class="collect-time">${relation.level3.time}</span></td>
                <td>
                    <span class="level-code level-4" onclick="queryRelatedCode('${relation.level4.code}')">
                        ${relation.level4.code}
                    </span>
                </td>
                <td><span class="collect-time">${relation.level4.time}</span></td>
            </tr>
        `;
    });
    
    tableHTML += `
            </tbody>
        </table>
    `;
    
    relationContent.innerHTML = tableHTML;
}

// 显示具体信息（合并产品信息和批次信息）
function displayDetailInfo(productData, batchData) {
    const detailInfo = document.getElementById('detailInfo');
    
    if (!productData && !batchData) {
        detailInfo.innerHTML = `
            <div class="empty-state">
                <p class="empty-text">暂无具体信息</p>
            </div>
        `;
        return;
    }
    
    let infoHTML = '';
    
    // 产品信息部分
    if (productData) {
        infoHTML += `
            <div class="info-field">
                <span class="field-label">产品编号</span>
                <span class="field-value link" onclick="viewProductDetails('${productData.productCode}')">${productData.productCode}</span>
            </div>
            <div class="info-field">
                <span class="field-label">产品名称</span>
                <span class="field-value product-name">${productData.productName}</span>
            </div>
            <div class="info-field">
                <span class="field-label">产品规格</span>
                <span class="field-value">${productData.productSpec}</span>
            </div>
            <div class="info-field">
                <span class="field-label">计量单位</span>
                <span class="field-value">${productData.measureUnit}</span>
            </div>
            <div class="info-field">
                <span class="field-label">包装层级</span>
                <span class="status-label package-level-1">${productData.packageLevel}</span>
            </div>
        `;
    }
    
    // 批次信息部分
    if (batchData) {
        infoHTML += `
            <div class="info-field">
                <span class="field-label">生产批次</span>
                <span class="field-value batch-number">${batchData.batchNumber}</span>
            </div>
            <div class="info-field">
                <span class="field-label">生产日期</span>
                <span class="field-value">${batchData.productionDate}</span>
            </div>
            <div class="info-field">
                <span class="field-label">生产班次</span>
                <span class="field-value">${batchData.productionShift}</span>
            </div>
            <div class="info-field">
                <span class="field-label">生产线</span>
                <span class="field-value">${batchData.productionLine}</span>
            </div>
            <div class="info-field">
                <span class="field-label">质检状态</span>
                <span class="status-label quality-pass">${batchData.qualityStatus}</span>
            </div>
            <div class="info-field">
                <span class="field-label">码状态</span>
                <span class="status-label code-normal">${batchData.codeStatus}</span>
            </div>
        `;
    }
    
    detailInfo.innerHTML = infoHTML;
}

// 显示无结果状态
function displayNoResult() {
    const relationContent = document.getElementById('relationContent');
    const detailInfo = document.getElementById('detailInfo');
    
    relationContent.innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">❌</div>
            <p class="empty-text">未找到该码信息</p>
        </div>
    `;
    
    detailInfo.innerHTML = `
        <div class="empty-state">
            <p class="empty-text">暂无具体信息</p>
        </div>
    `;
    
    updateRelationStats('未找到关联记录');
}

// 显示错误信息
function displayError(errorMessage) {
    const relationContent = document.getElementById('relationContent');
    
    relationContent.innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">⚠️</div>
            <p class="empty-text">查询异常</p>
            <p style="font-size: 14px; color: #999; margin-top: 8px;">${errorMessage}</p>
        </div>
    `;
    
    updateRelationStats('查询异常');
}

// 清空查询
function clearQuery() {
    const codeInput = document.getElementById('codeInput');
    codeInput.value = '';
    codeInput.focus();
    
    currentQueryCode = '';
    
    // 重置显示
    const relationContent = document.getElementById('relationContent');
    const detailInfo = document.getElementById('detailInfo');
    
    relationContent.innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">🔍</div>
            <p class="empty-text">请输入码进行查询</p>
        </div>
    `;
    
    detailInfo.innerHTML = `
        <div class="empty-state">
            <p class="empty-text">暂无具体信息</p>
        </div>
    `;
    
    updateQueryStatus('请输入码进行查询', 'default');
    updateRelationStats('未查询');
    
    Message.info('已清空查询内容');
}

// 开始扫码
function startScan() {
    Message.info('扫码功能正在开发中');
    // TODO: 实现扫码功能
}

// 查询关联码
function queryRelatedCode(code) {
    const codeInput = document.getElementById('codeInput');
    codeInput.value = code;
    performQuery();
}

// 查看产品详情
function viewProductDetails(productCode) {
    Message.info(`查看产品详情: ${productCode}`);
    // TODO: 实现产品详情查看功能
}

// 验证码格式
function validateCodeFormat(code) {
    // 简单的码格式验证
    const codePattern = /^L[1-4]_\d{3}_\d{4}$/;
    return codePattern.test(code);
}

// 更新查询状态
function updateQueryStatus(text, type = 'default') {
    const queryStatus = document.getElementById('queryStatus');
    const statusText = queryStatus.querySelector('.status-text');
    
    statusText.textContent = text;
    
    // 清除所有状态类
    queryStatus.classList.remove('loading', 'success', 'error');
    
    // 添加对应状态类
    if (type !== 'default') {
        queryStatus.classList.add(type);
    }
    
    // 添加加载动画
    if (type === 'loading') {
        statusText.innerHTML = `<span class="loading-spinner"></span> ${text}`;
    }
}

// 更新关联统计
function updateRelationStats(text) {
    const relationStats = document.getElementById('relationStats');
    const statsText = relationStats.querySelector('.stats-text');
    statsText.textContent = text;
}

// 设置按钮禁用状态
function setButtonsDisabled(disabled) {
    const queryBtn = document.getElementById('queryBtn');
    const clearBtn = document.getElementById('clearBtn');
    const scanBtn = document.getElementById('scanBtn');
    
    if (queryBtn) queryBtn.disabled = disabled;
    if (clearBtn) clearBtn.disabled = disabled;
    if (scanBtn) scanBtn.disabled = disabled;
}

// 添加到历史记录
function addToHistory(code, success) {
    const historyItem = {
        code: code,
        time: new Date().toLocaleString(),
        success: success
    };
    
    queryHistory.unshift(historyItem);
    
    // 限制历史记录数量
    if (queryHistory.length > 50) {
        queryHistory = queryHistory.slice(0, 50);
    }
    
    saveQueryHistory();
}

// 更新查询统计
function updateQueryStats(success) {
    queryStats.todayCount++;
    queryStats.totalCount++;
    
    if (success) {
        queryStats.successCount++;
    }
    
    // 更新显示
    const todayCount = document.getElementById('todayCount');
    const totalCount = document.getElementById('totalCount');
    const successRate = document.getElementById('successRate');
    
    if (todayCount) todayCount.textContent = queryStats.todayCount;
    if (totalCount) totalCount.textContent = queryStats.totalCount;
    if (successRate) {
        const rate = queryStats.totalCount > 0 ? 
            Math.round((queryStats.successCount / queryStats.totalCount) * 100) : 0;
        successRate.textContent = rate;
    }
    
    saveQueryStats();
}

// 保存查询历史
function saveQueryHistory() {
    try {
        localStorage.setItem('codeQueryHistory', JSON.stringify(queryHistory));
    } catch (error) {
        console.warn('保存查询历史失败:', error);
    }
}

// 加载查询历史
function loadQueryHistory() {
    try {
        const saved = localStorage.getItem('codeQueryHistory');
        if (saved) {
            queryHistory = JSON.parse(saved);
        }
    } catch (error) {
        console.warn('加载查询历史失败:', error);
        queryHistory = [];
    }
}

// 保存查询统计
function saveQueryStats() {
    try {
        localStorage.setItem('codeQueryStats', JSON.stringify(queryStats));
    } catch (error) {
        console.warn('保存查询统计失败:', error);
    }
}

// 加载查询统计
function loadQueryStats() {
    try {
        const saved = localStorage.getItem('codeQueryStats');
        if (saved) {
            const savedStats = JSON.parse(saved);
            
            // 检查是否是今天的数据
            const today = new Date().toDateString();
            const savedDate = savedStats.date;
            
            if (savedDate === today) {
                queryStats = savedStats;
            } else {
                // 新的一天，重置今日统计
                queryStats.todayCount = 0;
                queryStats.date = today;
            }
        } else {
            queryStats.date = new Date().toDateString();
        }
        
        // 更新显示
        const todayCount = document.getElementById('todayCount');
        const totalCount = document.getElementById('totalCount');
        const successRate = document.getElementById('successRate');
        
        if (todayCount) todayCount.textContent = queryStats.todayCount;
        if (totalCount) totalCount.textContent = queryStats.totalCount;
        if (successRate) {
            const rate = queryStats.totalCount > 0 ? 
                Math.round((queryStats.successCount / queryStats.totalCount) * 100) : 0;
            successRate.textContent = rate;
        }
        
    } catch (error) {
        console.warn('加载查询统计失败:', error);
        queryStats = {
            todayCount: 0,
            totalCount: 0,
            successCount: 0,
            date: new Date().toDateString()
        };
    }
}

// 开始时间更新
function startTimeUpdate() {
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
        
        const currentTimeElement = document.getElementById('currentTime');
        if (currentTimeElement) {
            currentTimeElement.textContent = timeString;
        }
    }
    
    updateTime();
    setInterval(updateTime, 1000);
}



function showErrorDialog(message, details = '') {
    const dialog = document.getElementById('errorDialog');
    const errorMessage = document.getElementById('errorMessage');
    const errorDetails = document.getElementById('errorDetails');
    
    if (dialog && errorMessage) {
        errorMessage.textContent = message;
        if (errorDetails) {
            errorDetails.textContent = details;
            errorDetails.style.display = details ? 'block' : 'none';
        }
        dialog.style.display = 'flex';
    }
}

function closeErrorDialog() {
    const dialog = document.getElementById('errorDialog');
    if (dialog) {
        dialog.style.display = 'none';
    }
}

function retryQuery() {
    closeErrorDialog();
    performQuery();
}

// 检查URL参数
function checkUrlParams() {
    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    
    if (code) {
        const codeInput = document.getElementById('codeInput');
        if (codeInput) {
            codeInput.value = code;
            // 延迟执行查询，确保页面完全加载
            setTimeout(() => {
                performQuery();
            }, 500);
        }
    }
}

// 导出全局函数供HTML调用
window.performQuery = performQuery;
window.clearQuery = clearQuery;
window.startScan = startScan;
window.queryRelatedCode = queryRelatedCode;
window.viewProductDetails = viewProductDetails;
window.showErrorDialog = showErrorDialog;
window.closeErrorDialog = closeErrorDialog;
window.retryQuery = retryQuery; 