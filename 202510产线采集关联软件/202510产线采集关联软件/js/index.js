// 主页面JavaScript功能

// 时间显示功能
function updateTime() {
    const now = new Date();
    const timeString = Utils.formatDate(now, 'YYYY-MM-DD HH:mm:ss');
    const timeElement = document.getElementById('currentTime');
    if (timeElement) {
        timeElement.textContent = timeString;
    }
}

// 导航功能
function initNavigation() {
    const navLinks = document.querySelectorAll('.nav-link');
    
    navLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            // 移除所有活动状态
            navLinks.forEach(l => l.classList.remove('active'));
            // 添加当前活动状态
            this.classList.add('active');
        });
    });
}

// 功能卡片交互
function initFeatureCards() {
    const cards = document.querySelectorAll('.feature-card');
    
    cards.forEach(card => {
        card.addEventListener('click', function() {
            const button = this.querySelector('.card-button');
            if (button) {
                window.location.href = button.href;
            }
        });
    });
}

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    // 初始化时间显示
    updateTime();
    setInterval(updateTime, 1000);
    
    // 初始化导航
    initNavigation();
    
    // 初始化功能卡片
    initFeatureCards();
    
    // 添加欢迎消息
    setTimeout(() => {
        Message.info('欢迎使用产线采集关联软件！', 2000);
    }, 1000);
});

// 添加活动状态样式
const style = document.createElement('style');
style.textContent = `
    .nav-link.active {
        background-color: #1890ff !important;
        color: white !important;
        border-left-color: #40a9ff !important;
    }
    
    .feature-card {
        cursor: pointer;
    }
`;
document.head.appendChild(style); 