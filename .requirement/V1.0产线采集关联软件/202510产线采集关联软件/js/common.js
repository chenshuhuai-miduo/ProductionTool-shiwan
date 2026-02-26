// 通用JavaScript功能

// 消息提示功能
const Message = {
    show(text, type = 'info', duration = 3000) {
        const message = document.createElement('div');
        message.className = `message ${type}`;
        message.textContent = text;
        
        document.body.appendChild(message);
        
        setTimeout(() => {
            message.style.animation = 'slideOut 0.3s ease forwards';
            setTimeout(() => {
                document.body.removeChild(message);
            }, 300);
        }, duration);
    },
    
    success(text, duration) {
        this.show(text, 'success', duration);
    },
    
    error(text, duration) {
        this.show(text, 'error', duration);
    },
    
    warning(text, duration) {
        this.show(text, 'warning', duration);
    },
    
    info(text, duration) {
        this.show(text, 'info', duration);
    }
};

// 添加slideOut动画
const style = document.createElement('style');
style.textContent = `
    @keyframes slideOut {
        to {
            transform: translateX(100%);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// 模态框功能
const Modal = {
    show(content, options = {}) {
        const modal = document.createElement('div');
        modal.className = 'modal';
        
        const modalContent = document.createElement('div');
        modalContent.className = 'modal-content';
        modalContent.style.width = options.width || '600px';
        
        const header = document.createElement('div');
        header.className = 'modal-header';
        
        const title = document.createElement('h3');
        title.className = 'modal-title';
        title.textContent = options.title || '提示';
        
        const closeBtn = document.createElement('button');
        closeBtn.className = 'modal-close';
        closeBtn.innerHTML = '×';
        closeBtn.onclick = () => this.hide(modal);
        
        header.appendChild(title);
        header.appendChild(closeBtn);
        
        const body = document.createElement('div');
        body.className = 'modal-body';
        if (typeof content === 'string') {
            body.innerHTML = content;
        } else {
            body.appendChild(content);
        }
        
        modalContent.appendChild(header);
        modalContent.appendChild(body);
        
        if (options.footer !== false) {
            const footer = document.createElement('div');
            footer.className = 'modal-footer';
            
            if (options.buttons) {
                options.buttons.forEach(btn => {
                    const button = document.createElement('button');
                    button.className = `btn ${btn.class || 'btn-secondary'}`;
                    button.textContent = btn.text;
                    button.onclick = () => {
                        if (btn.onclick) btn.onclick();
                        if (btn.close !== false) this.hide(modal);
                    };
                    footer.appendChild(button);
                });
            } else {
                const okBtn = document.createElement('button');
                okBtn.className = 'btn btn-primary';
                okBtn.textContent = '确定';
                okBtn.onclick = () => this.hide(modal);
                footer.appendChild(okBtn);
            }
            
            modalContent.appendChild(footer);
        }
        
        modal.appendChild(modalContent);
        document.body.appendChild(modal);
        
        // 点击背景关闭
        modal.onclick = (e) => {
            if (e.target === modal) {
                this.hide(modal);
            }
        };
        
        return modal;
    },
    
    hide(modal) {
        if (modal && modal.parentNode) {
            document.body.removeChild(modal);
        }
    },
    
    confirm(message, callback, options = {}) {
        const modal = this.show(message, {
            title: options.title || '确认',
            width: options.width || '400px',
            buttons: [
                {
                    text: '取消',
                    class: 'btn-secondary'
                },
                {
                    text: '确定',
                    class: 'btn-primary',
                    onclick: callback
                }
            ]
        });
        return modal;
    }
};

// 表单验证功能
const Validator = {
    rules: {
        required: (value) => value && value.trim() !== '',
        email: (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value),
        phone: (value) => /^1[3-9]\d{9}$/.test(value),
        number: (value) => !isNaN(value) && value !== '',
        min: (value, min) => parseFloat(value) >= min,
        max: (value, max) => parseFloat(value) <= max,
        minLength: (value, length) => value && value.length >= length,
        maxLength: (value, length) => value && value.length <= length,
        pattern: (value, pattern) => new RegExp(pattern).test(value)
    },
    
    messages: {
        required: '此字段为必填项',
        email: '请输入有效的邮箱地址',
        phone: '请输入有效的手机号码',
        number: '请输入有效的数字',
        min: '值不能小于 {0}',
        max: '值不能大于 {0}',
        minLength: '长度不能少于 {0} 个字符',
        maxLength: '长度不能超过 {0} 个字符',
        pattern: '格式不正确'
    },
    
    validate(element, rules) {
        const value = element.value;
        const errors = [];
        
        for (const rule of rules) {
            const [ruleName, ...params] = rule.split(':');
            const ruleFunc = this.rules[ruleName];
            
            if (ruleFunc) {
                const isValid = params.length > 0 
                    ? ruleFunc(value, ...params) 
                    : ruleFunc(value);
                
                if (!isValid) {
                    let message = this.messages[ruleName] || '验证失败';
                    params.forEach((param, index) => {
                        message = message.replace(`{${index}}`, param);
                    });
                    errors.push(message);
                }
            }
        }
        
        return errors;
    },
    
    validateForm(form) {
        const errors = {};
        const elements = form.querySelectorAll('[data-validate]');
        
        elements.forEach(element => {
            const rules = element.dataset.validate.split('|');
            const fieldErrors = this.validate(element, rules);
            
            if (fieldErrors.length > 0) {
                errors[element.name || element.id] = fieldErrors;
                this.showFieldError(element, fieldErrors[0]);
            } else {
                this.clearFieldError(element);
            }
        });
        
        return Object.keys(errors).length === 0 ? null : errors;
    },
    
    showFieldError(element, message) {
        element.classList.add('error');
        
        let errorElement = element.parentNode.querySelector('.form-error');
        if (!errorElement) {
            errorElement = document.createElement('div');
            errorElement.className = 'form-error';
            element.parentNode.appendChild(errorElement);
        }
        errorElement.textContent = message;
    },
    
    clearFieldError(element) {
        element.classList.remove('error');
        
        const errorElement = element.parentNode.querySelector('.form-error');
        if (errorElement) {
            errorElement.remove();
        }
    }
};

// 加载状态管理
const Loading = {
    show(element, text = '加载中...') {
        if (typeof element === 'string') {
            element = document.querySelector(element);
        }
        
        element.disabled = true;
        element.innerHTML = `
            <span class="loading">
                <span class="loading-spinner"></span>
                ${text}
            </span>
        `;
    },
    
    hide(element, originalText) {
        if (typeof element === 'string') {
            element = document.querySelector(element);
        }
        
        element.disabled = false;
        element.innerHTML = originalText;
    }
};

// 工具函数
const Utils = {
    // 格式化日期
    formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
        if (!date) return '';
        
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        const seconds = String(d.getSeconds()).padStart(2, '0');
        
        return format
            .replace('YYYY', year)
            .replace('MM', month)
            .replace('DD', day)
            .replace('HH', hours)
            .replace('mm', minutes)
            .replace('ss', seconds);
    },
    
    // 格式化数字
    formatNumber(num, decimals = 0) {
        if (isNaN(num)) return '0';
        return Number(num).toLocaleString('zh-CN', {
            minimumFractionDigits: decimals,
            maximumFractionDigits: decimals
        });
    },
    
    // 防抖函数
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },
    
    // 节流函数
    throttle(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },
    
    // 深拷贝
    deepClone(obj) {
        return JSON.parse(JSON.stringify(obj));
    },
    
    // 生成UUID
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }
};

// 包装比例工具
const PackageUtils = {
    // 解析包装比例配置
    parsePackageRatio(ratio) {
        if (!ratio) return null;
        
        const parts = ratio.split(':').map(p => parseInt(p.trim()));
        
        if (parts.length === 1) {
            return {
                level: 2,
                ratios: [parts[0]],
                mode: '二级关联',
                description: `单品码 → 虚拟垛标 (${parts[0]}个/垛)`
            };
        } else if (parts.length === 2) {
            return {
                level: 3,
                ratios: parts,
                mode: '三级关联',
                description: `瓶码 → 箱码 → 虚拟垛标 (${parts[0]}个/箱, ${parts[1]}箱/垛)`
            };
        } else if (parts.length === 3) {
            return {
                level: 4,
                ratios: parts,
                mode: '四级关联',
                description: `瓶码 → 盒码 → 箱码 → 虚拟垛标 (${parts[0]}个/盒, ${parts[1]}盒/箱, ${parts[2]}箱/垛)`
            };
        }
        
        return null;
    },
    
    // 获取包装比例标签类
    getPackageTagClass(level) {
        const classes = {
            2: 'level-2',
            3: 'level-3',
            4: 'level-4'
        };
        return classes[level] || 'level-2';
    },
    
    // 计算总数量
    calculateTotal(ratios) {
        return ratios.reduce((total, ratio) => total * ratio, 1);
    }
};

// 状态管理
const StatusUtils = {
    // 获取状态标签类
    getStatusClass(status) {
        const statusMap = {
            '启用': 'success',
            '禁用': 'default',
            '草稿': 'warning',
            '待开始': 'default',
            '进行中': 'success',
            '已暂停': 'warning',
            '已完成': 'info',
            '已停止': 'error',
            '异常': 'error',
            '成功': 'success',
            '失败': 'error'
        };
        return statusMap[status] || 'default';
    }
};

// 全局事件处理
document.addEventListener('DOMContentLoaded', function() {
    // 自动初始化表单验证
    const forms = document.querySelectorAll('form[data-validate]');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            const errors = Validator.validateForm(form);
            if (!errors) {
                // 表单验证通过，可以提交
                console.log('表单验证通过');
            }
        });
        
        // 实时验证
        const elements = form.querySelectorAll('[data-validate]');
        elements.forEach(element => {
            element.addEventListener('blur', function() {
                const rules = element.dataset.validate.split('|');
                const errors = Validator.validate(element, rules);
                
                if (errors.length > 0) {
                    Validator.showFieldError(element, errors[0]);
                } else {
                    Validator.clearFieldError(element);
                }
            });
        });
    });
    
    // 自动关闭消息提示
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('message')) {
            e.target.style.animation = 'slideOut 0.3s ease forwards';
            setTimeout(() => {
                if (e.target.parentNode) {
                    e.target.parentNode.removeChild(e.target);
                }
            }, 300);
        }
    });
});

// 通用导航功能 - 兼容本地和服务器环境
const Navigation = {
    // 获取当前页面的基础路径
    getBasePath() {
        const currentPath = window.location.pathname;
        const currentDir = currentPath.substring(0, currentPath.lastIndexOf('/'));
        return currentDir;
    },

    // 导航到指定页面
    navigateTo(pageName) {
        console.log(`导航到页面: ${pageName}`);
        try {
            const basePath = this.getBasePath();
            const targetUrl = basePath + '/' + pageName;
            console.log('目标URL:', targetUrl);
            window.location.href = targetUrl;
        } catch (error) {
            console.error('导航失败:', error);
            // 备用方案：使用相对路径
            window.location.href = pageName;
        }
    },

    // 返回主页
    navigateToIndex() {
        this.navigateTo('index.html');
    },

    // 导航到查询码页面
    navigateToCodeQuery() {
        this.navigateTo('code-query.html');
    },

    // 导航到码替换页面
    navigateToCodeReplace() {
        this.navigateTo('code-replace.html');
    },

    // 导航到产品管理页面
    navigateToProductManagement() {
        this.navigateTo('product-management.html');
    },

    // 导航到任务管理页面
    navigateToTaskList() {
        this.navigateTo('task-list.html');
    },

    // 导航到系统配置页面
    navigateToSystemConfig() {
        this.navigateTo('system-config.html');
    },

    // 导航到数据上传页面
    navigateToDataUpload() {
        this.navigateTo('data-upload.html');
    }
};

// 全局导航函数（向后兼容）
function navigateToIndex() {
    Navigation.navigateToIndex();
}

function navigateToCodeQuery() {
    Navigation.navigateToCodeQuery();
}

function navigateToCodeReplace() {
    Navigation.navigateToCodeReplace();
}

function navigateToProductManagement() {
    Navigation.navigateToProductManagement();
}

function navigateToTaskList() {
    Navigation.navigateToTaskList();
}

function navigateToSystemConfig() {
    Navigation.navigateToSystemConfig();
}

function navigateToDataUpload() {
    Navigation.navigateToDataUpload();
}

// 导出到全局
window.Message = Message;
window.Modal = Modal;
window.Validator = Validator;
window.Loading = Loading;
window.Utils = Utils;
window.PackageUtils = PackageUtils;
window.StatusUtils = StatusUtils;
window.Navigation = Navigation;
window.navigateToIndex = navigateToIndex;
window.navigateToCodeQuery = navigateToCodeQuery;
window.navigateToCodeReplace = navigateToCodeReplace;
window.navigateToProductManagement = navigateToProductManagement;
window.navigateToTaskList = navigateToTaskList;
window.navigateToSystemConfig = navigateToSystemConfig;
window.navigateToDataUpload = navigateToDataUpload;

// 导航系统初始化完成
console.log('导航系统已初始化'); 