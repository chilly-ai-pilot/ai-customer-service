## Iteration 1：双端账号体系

**目的**

用户和商户是两套独立账号体系（分表原因见"选型"）。本迭代交付最小闭环：双方各自能注册、登录，密码不落明文。不涉及登录态维持、权限、任何业务功能。

**选型**

| 项 | 选择 | 理由 |
|---|---|---|
| 数据模型 | `User`、`CommercialTenant` 两张独立表，各自 `id/account/password/name` | 两套身份天然独立，不共享任何字段语义；商户后续会挂商品、知识库、AI配置等一整套用户不会有的专属资产，分表能避免每加一个商户功能就要纠结"用户表要不要也加个字段" |
| 密码存储 | SHA-256 单向哈希，**不加盐** | 比明文有本质提升，但无盐哈希仍怕彩虹表，只是过渡方案；加盐+BCrypt留给安全加固迭代 |
| 账号唯一性 | 应用层查重，不依赖数据库唯一索引 | 唯一索引是数据库层防御措施，可以晚一点加；本迭代先保证业务逻辑正确 |
| 登录态 | 登录成功只返回用户信息，不签发任何凭证 | 没有需要鉴权保护的资源，提前做token是过度设计 |
| 错误码 | 1001=账号已存在 / 1002=账号不存在 / 1003=密码错误 / 1004=参数错误，用四个不同编号区分 | 后续所有登录相关排错、前端提示文案的基础 |

**关键约束**

1. 两套账号体系必须完全独立：同一个 `account` 字符串可以同时是用户和商户的账号，互不冲突
2. 账号唯一性只在各自体系内约束，跨表允许重复
3. 注册/登录失败必须是明确的业务错误码，不能是服务端异常导致500
4. 登录失败要能区分"账号不存在"和"密码错误"，两套体系逻辑必须一致
5. 密码永远不出现在返回体里，且校验方式必须是"输入密码同样哈希后比对"，不能反解或明文比对

**本次不做**

- 加盐（留给安全加固迭代）、token/JWT机制、账号唯一性数据库约束
- 找回密码、修改密码、注册审核流程、账号运营后台
- 并发查重竞态：先查重、再插入

**验收命令**

**1. 用户注册成功，且密码不以明文落库**
```bash
curl -s -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_user","password":"123456","name":"测试用户"}'
```
→ 断言：返回成功，返回体包含新用户 `id`，不包含 `password`；查库确认 `password` 是64位十六进制字符串，不等于明文

**2. 重复注册被拒绝**
```bash
curl -s -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_user","password":"123456","name":"测试用户"}'
```
→ 断言：返回"账号已存在"错误码，不是500，不产生第二条记录

**3. 正确密码登录成功**
```bash
curl -s -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_user","password":"123456"}'
```
→ 断言：返回登录成功，`password` 字段为空/不存在

**4. 错误密码登录失败，错误码与"账号不存在"不同**
```bash
curl -s -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_user","password":"wrong_password"}'
```
→ 断言：返回"密码错误"错误码，与账号不存在的错误码不同

**5. 账号不存在登录失败**
```bash
curl -s -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"account":"never_registered","password":"whatever"}'
```
→ 断言：返回"账号不存在"错误码

**6. 用哈希值直接当密码登录，必须失败**
```bash
# 用步骤1查库取到的哈希值 HASH_VALUE
curl -s -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_user","password":"HASH_VALUE"}'
```
→ 断言：登录失败（证明校验逻辑是"输入做哈希再比对"，不是字符串相等）

**7. 商户入驻流程（重复1-6）**
```bash
curl -s -X POST http://localhost:8080/commercialTenant/register \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_shop","password":"123456","name":"测试商户"}'
curl -s -X POST http://localhost:8080/commercialTenant/register \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_shop","password":"123456","name":"测试商户"}'
curl -s -X POST http://localhost:8080/commercialTenant/login \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_shop","password":"123456"}'
curl -s -X POST http://localhost:8080/commercialTenant/login \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_shop","password":"wrong_password"}'
curl -s -X POST http://localhost:8080/commercialTenant/login \
  -H "Content-Type: application/json" \
  -d '{"account":"never_registered_shop","password":"whatever"}'
```
→ 断言：五条结果对应步骤1-5同样预期

**8. 两套体系互不冲突**
```bash
curl -s -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"account":"cross_test","password":"pwd_user","name":"用户身份"}'
curl -s -X POST http://localhost:8080/commercialTenant/register \
  -H "Content-Type: application/json" \
  -d '{"account":"cross_test","password":"pwd_shop","name":"商户身份"}'
curl -s -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"account":"cross_test","password":"pwd_user"}'
curl -s -X POST http://localhost:8080/commercialTenant/login \
  -H "Content-Type: application/json" \
  -d '{"account":"cross_test","password":"pwd_shop"}'
curl -s -X POST http://localhost:8080/commercialTenant/login \
  -H "Content-Type: application/json" \
  -d '{"account":"cross_test","password":"pwd_user"}'
```
→ 断言：前两条都注册成功；第三、四条各自登录成功且返回各自`name`；第五条用user密码登录commercialTenant必须失败

**9. 参数缺失不能导致500**
```bash
curl -s -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"account":"","password":""}'
curl -s -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{}'
curl -s -X POST http://localhost:8080/commercialTenant/register \
  -H "Content-Type: application/json" \
  -d '{"account":"","password":""}'
```
→ 断言：均返回明确参数错误码，不是500，不产生记录

**10. 两套体系哈希算法一致**
```bash
curl -s -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"account":"hash_check_user","password":"same_password_123","name":"哈希校验用户"}'
curl -s -X POST http://localhost:8080/commercialTenant/register \
  -H "Content-Type: application/json" \
  -d '{"account":"hash_check_shop","password":"same_password_123","name":"哈希校验商户"}'
```
→ 断言：查库比对两条记录的 `password` 字段值完全相同