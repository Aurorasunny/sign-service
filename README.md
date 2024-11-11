### 电子签章服务
本系统主要针对pdf格式或图片格式的文件进行电子签章，并附带oss对象存储功能

> 实现功能

- [x] 文件上传（本地文件/云供应商）
- [x] 对图片/pdf格式文件的指定**坐标**进行电子签章
- [x] 对图片/pdf格式文件的指定**关键词**进行电子签章
- [x] 基于PaddleOCR查找图片上指定关键词的位置
- [x] 对签章的数据进行数字签名，并嵌入文件内

**注：**图片只支持png/jpg格式的文件，数字签名只支持jpg格式

> 待开发功能 - 支线功能

**注：**本项目的初衷是尽可能的少的涉及管理相关的内容，打造一个通用的模块服务，管理以及认证相关的内容尽可能在你们自己原有项目上进行，故一下的功能不会引入主分支

- [ ] 引入用户管理功能
- [ ] 对印章和签名信息进行统一内部管理
- [ ] 引入工作流对印章和签名信息进行统一认证
- [ ] 电子签章时只有内部认证通过的印章和签名才能进行签章
