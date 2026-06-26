

Based on the code map provided, I have comprehensive information about this project. Let me create the README.md file based on the project structure:

# Smart Medicine - 智能医疗系统

Smart Medicine 是一款功能完善的智能医疗健康管理系统，采用 Spring Boot + MyBatis-Plus + MySQL 技术栈构建。该系统提供在线问诊、药品查询、医学影像智能分析、AI 智能问答等核心功能。

## 功能特点

### 🏥 核心功能
- **在线问诊** - 用户可查看疾病类型、症状，进行在线病情咨询
- **药品管理** - 完整的药品信息库，支持药品搜索、购买
- **医学影像分析** - AI 智能识别分析医学报告单（支持 CT、MRI、X光等影像）
- **AI 智能问答** - 基于 RAG（检索增强生成）技术的 AI 医疗助手
- **订单管理** - 药品购买订单与支付功能（支持支付宝）
- **用户中心** - 个人资料管理、订单历史查看

### 💡 技术亮点
- AI 大模型集成（阿里云通义千问）
- RAG 知识增强问答
- 医学影像智能分析
- 异步任务处理
- 阿里云 OSS 对象存储
- 邮箱验证

## 技术栈

| 分类 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7.x |
| 数据库 | MySQL + MyBatis-Plus |
| AI | 通义千问 Qwen API |
| 存储 | 阿里云 OSS |
| 支付 | 支付宝开放平台 |
| 邮件 | JavaMail |

## 项目结构

```
src/main/java/world/
├── SmartMedicineApplication.java    # 应用入口
├── component/                        # 组件
│   ├── EmailClient.java              # 邮件客户端
│   ├── LoginHandlerInterceptor.java # 登录拦截器
│   └── OssClient.java                # OSS 对象存储
├── config/                          # 配置类
│   ├── AlipayConfig.java            # 支付宝配置
│   ├── AsyncConfig.java             # 异步任务配置
│   ├── DatabaseMigrationRunner.java # 数据库迁移
│   ├── MvcConfig.java               # MVC 配置
│   └── MybatisPlusConfig.java        # MyBatis-Plus 配置
├── controller/                      # 控制器
│   ├── LoginController.java        # 登录
│   ├── UserController.java         # 用户
│   ├── MedicineController.java    # 药品
│   ├── IllnessController.java   # 疾病
│   ├── MedicalScanController.java # 医学影像
│   ├── MessageController.java    # AI 问答
│   ├── OrderController.java     # 订单
│   └── AlipayController.java    # 支付
├── service/                       # 业务服务
│   ├── ApiService.java           # AI API 服务
│   ├── MedicalRecordAnalysisService.java # 影像分析
│   ├── KnowledgeRetrievalService.java   # RAG 知识检索
│   └── ...
├── dao/                          # 数据访问
├── entity/                      # 实体类
├── dto/                        # 数据传输对象
└── utils/                     # 工具类
```

## 快速开始

### 环境要求
- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+

### 配置步骤

1. **克隆项目**
```bash
git clone https://gitee.com/cyhcyx/auto-hospital-system.git
```

2. **创建数据库**
```sql
CREATE DATABASE smart_medicine DEFAULT CHARACTER SET utf8mb4;
```

3. **导入数据**
```bash
mysql -u root -p smart_medicine < src/main/resources/smart-medicine.sql
```

4. **修改配置文件**

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smart_medicine
    username: your_username
    password: your_password
  mail:
    username: your_email@qq.com
    password: your_auth_code

# AI API Key (阿里云通义千问)
ai-key: your_api_key

# 阿里云 OSS
oss:
  access-key: your_access_key
  access-secret: your_access_secret
  bucket-name: your_bucket
  end-point: your_endpoint

# 支付宝
alipay:
  app-id: your_app_id
  merchant-private-key: your_private_key
  alipay-public-key: alipay_public_key
  gateway-url: https://openapi.alipaydev.com/gateway.do
```

5. **运行项目**
```bash
mvn clean install
java -jar target/smart-medicine-1.0.jar
```

访问 `http://localhost:8080` 即可进入系统。

## 主要页面

| 路由 | 说明 |
|------|------|
| `/` | 首页 |
| `/doctor` | 医生/问诊页面 |
| `/findIllness` | 疾病查询 |
| `/findMedicines` | 药品查询 |
| `/medical-scan` | 医学影像分析 |
| `/orders` | 订单中心 |
| `/profile` | 个人中心 |

## API 接口

### 用户接口
- `POST /login/login` - 用户登录
- `POST /login/register` - 用户注册
- `POST /login/sendEmailCode` - 发送邮箱验证码

### 药品接口
- `GET /medicine/list` - 药品列表
- `GET /medicine/{id}` - 药品详情

### 疾病接口
- `GET /illness/list` - 疾病列表
- `GET /illness/{id}` - 疾病详情

### 医学影像
- `POST /api/medical-record/analyze` - 分析医学影像
- `GET /api/analysis-history/list` - 分析历史

### AI 问答
- `POST /message/query` - AI 问答
- `GET /message/query/stream` - 流式问答

### 订单支付
- `POST /order/create` - 创建订单
- `POST /order/pay` - 支付订单
- `POST /order/alipay/pay` - 支付宝支付

## 许可证

本项目仅供学习交流使用。

## 贡献者

欢迎提交 Issue 和 Pull Request！