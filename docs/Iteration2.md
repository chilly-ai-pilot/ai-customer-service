# Iteration 2：商户工作台 v0 —— 商品管理 + 极简 token 校验

**目的**

商户登录后需要一个落脚点。本迭代交付两块相对独立、可分别验收的内容:一是工作台菜单骨架(先有壳子);二是在壳子内实现商品管理——商户能看到自己名下的商品列表,能新增/编辑/删除商品,且这些操作必须归属到自己名下,别人碰不了。商品是后面知识库、AI配置的挂载点,也是工作台里第一个真正的"资产"。商品的**公开浏览**(顾客视角)不属于本迭代范围,本迭代自行实现 `/goods/all` 公开列表,迭代3的顾客侧接口可复用同一套查询逻辑。

**选型**

| 项 | 选择 | 理由 |
|---|---|---|
| 商品模型 | `Goods` 表,关联 `ctId` | 商品归属明确,是后续知识库/AI配置的挂载对象 |
| 身份校验 | 登录成功签发一个随机字符串token,服务端维护token→ctId的映射;工作台**写操作**接口统一校验"token对应的ctId == 操作目标所属的ctId" | 不追求JWT的完整性(签名、过期),先解决"至少不能让商户A随手改商户B的商品"这道最低门槛,JWT留给安全加固迭代 |
| **Token 存储结构** | **`/user/login` 与 `/commercialTenant/login` 均签发token,但服务端用两个独立的Map分别维护:`tenantToken → ctId` 与 `userToken → userId`,不共用同一张表** | **两类token的语义不同(一个映射ctId,一个映射userId),分开存储可以从根本上避免"用户token被商户接口误当成有效token使用"这类混淆;商户相关接口只查`tenantToken`表,查不到即视为token不存在,即使该字符串是一个合法的用户token也一样按UNAUTHORIZED处理** |
| **新增商品的 ctId 归属** | **`/goods/add` 的商品 `ctId` 一律以 token 解析出的 ctId 为准,忽略请求体中传入的 `ctId` 字段(即使传了也不生效)** | **新增时商品尚不存在,无法像修改/删除那样拿"已有ctId"跟token比对;如果信任请求体,商户A可以在body里伪造商户B的ctId,变相创建出挂在他人名下的商品,这与约束1的精神冲突** |
| **商品详情接口的归属** | **详情接口(`/goods/detail`等)不做任何基于token的过滤,无论是否携带token、携带谁的token,均返回完整商品信息,行为与本迭代 `/goods/all` 一致** | **当前商品模型没有任何商户私有字段,详情数据本身就是完全公开的,不存在"信息该不该被看到"的问题;仅列表接口做token归属过滤,是因为列表要区分"工作台视图(看自己的)"和"公开视图(看全部)",这是视图范围的区别,不是数据保密的区别。若未来商品模型引入商户私有字段(如成本价、库存预警线等),需重新评估详情接口是否要做字段级过滤** |
| **商品列表接口拆分** | 商品列表拆成两个独立接口:`/goods/mine` 只返回token对应商户名下的商品,**必须携带有效token,否则返回UNAUTHORIZED**;`/goods/all` 返回全部商户的商品,是纯公开接口,不做任何token相关处理 | 不再用"同一个接口靠token分流"这种隐式规则,接口名直接表达语义;商户登录后随时能调`/goods/all`看全部商户商品,不会像"单接口分流"方案那样一登录就看不到别人的商品了;`/goods/all`完全不掺token逻辑,和迭代3公开浏览彻底解耦 |
| **Token登录覆盖策略** | 同一商户重复登录时,新token直接顶替该商户名下的旧token,旧token立即失效;**用户侧同样独立执行这一策略(用户重复登录顶替旧的userToken),两套体系互不影响** | 不支持同一账号多端同时在线,服务端只需维护一份"当前有效token",实现和排查都更简单,MVP阶段够用 |
| **登录接口返回契约** | `/user/login` 与 `/commercialTenant/login` 响应体统一在 `data.token` 字段返回新签发的token,字段名和位置两边保持一致;不考虑与迭代1的向后兼容 | 统一字段位置便于前端复用同一套处理逻辑 |
| 工作台信息架构 | 明确菜单骨架:商品管理/知识库/AI设置/会话收件箱/经营数据,未实现的模块先占位 | 提前规划信息架构,避免每加一个功能就临时拍一个入口,导致后台越做越乱 |
| **校验顺序** | 先判断token是否存在,不存在返回401(UNAUTHORIZED);存在但ctId与操作目标不匹配,返回403(FORBIDDEN) | 明确校验顺序,保证两类错误码各走各的分支,不能混用 |

**关键约束**

1. 商户在工作台内只能查看/操作自己名下的商品,不能通过任何写操作接口触达他人的商品数据。**新增商品时的归属判定同样适用本约束——ctId 必须来自token,不能来自请求体。**
2. 未携带token或携带一个不存在的token时:
   - 请求**写操作**(新增/修改/删除)或 **`/goods/mine`**,一律拒绝,不能创建出无主商品,也不能在没有身份的情况下回答"我的商品是什么",返回"未登录"性质的错误码(**UNAUTHORIZED**,编号401)
   - 请求**`/goods/all` 或商品详情**,视为公开浏览请求,正常返回全部商品/完整详情,不报错,不因token缺失或无效而受任何影响
   - **`/goods/all` 与详情接口不因token有效性而报错,是因为它们本身就是公开接口,不涉及"身份"概念,和"读操作默认宽松"这条规则无关,即使传了有效token也不做任何过滤;不适用于工作台菜单接口(见约束6)**
3. token有效但对应的ctId与操作目标不匹配时,返回"无权限"性质的错误码(**FORBIDDEN**,编号403),与约束2的"未登录"错误码区分开,不能混用同一个笼统的"拒绝"提示
4. token本身本迭代不需要过期机制,但必须能唯一、准确地映射回一个ctId(商户token)或userId(用户token);**重复登录会使同一账号的旧token失效,不支持多端同时在线。两类token分开存储,商户接口只认`tenantToken`表,用户token即使字符串合法也不能通过商户接口的校验,视同token不存在。**
5. Token 不存在(含伪造、含类型不匹配,例如拿用户token访问商户接口)与 Token 对应的 ctId 与操作目标不匹配,必须走两个不同的异常分支,前者返回 UNAUTHORIZED,后者返回 FORBIDDEN。先校验token是否存在,再校验ctId归属,不允许在第一步校验 token 不存在时直接抛 FORBIDDEN。
6. **工作台菜单接口(`/workbench/menu`)不是公开数据,无论读写都要求携带有效的商户token,否则返回UNAUTHORIZED(401)。"未携带"与"携带一个查无对应记录的伪造token"两种情况一视同仁,统一按UNAUTHORIZED处理,菜单接口不存在归属判断,因此不涉及FORBIDDEN分支。**

**ErrorCodes 补充**

本迭代新增以下错误码,写入 `ErrorCodes.java`:

```java
public static final int UNAUTHORIZED = 401;
public static final int FORBIDDEN    = 403;
public static final String MSG_UNAUTHORIZED = "未登录";
public static final String MSG_FORBIDDEN    = "无权限";
```

**本次不做**

- JWT签名与过期机制、密码加密升级(留给安全加固迭代)
- token持久化——当前token只存在服务内存中,服务重启后所有商户/用户的登录状态失效,需重新登录,留给安全加固迭代一并评估是否上Redis
- 同一账号多端同时登录
- 菜单骨架里"知识库/AI设置/收件箱/经营数据"这几个占位模块的具体业务逻辑(后续迭代逐个填)
- 商品详情接口的字段级权限控制(当前商品模型无私有字段,暂不需要)
- 商户资料编辑(店铺简介、logo等)

**验收命令**

*验收前置:确认 `/commercialTenant/login` 已返回 `data.token`,后续用例中的 `token_A`/`token_B` 均取自登录响应的 `data.token` 字段*

*准备:登录获取token*
```bash
# 商户A登录,得到 token_A
curl -s -X POST http://localhost:8080/commercialTenant/login \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_shop_a","password":"123456"}'
# 取返回的 data.token 作为 token_A

# 商户B登录,得到 token_B
curl -s -X POST http://localhost:8080/commercialTenant/login \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_shop_b","password":"123456"}'
# 取返回的 data.token 作为 token_B
```
→ 断言:两次均返回成功,`data.token` 字段均为非空字符串

*子目标A:工作台菜单骨架(独立验收,不依赖商品管理是否完成)*

**1. 工作台菜单骨架可访问**
```bash
curl -s -X GET "http://localhost:8080/workbench/menu" -H "Authorization: token_A"
```
→ 断言:返回包含"商品管理/知识库/AI设置/会话收件箱/经营数据"五个菜单项,未实现模块标注为占位状态而非报错

**1b. 不带token或伪造token访问菜单,拒绝访问**
```bash
curl -s -X GET "http://localhost:8080/workbench/menu"
curl -s -X GET "http://localhost:8080/workbench/menu" -H "Authorization: fake_token_xyz"
```
→ 断言:两次均返回**UNAUTHORIZED**(401),不返回菜单内容

*子目标B:商品管理(独立验收,不依赖菜单骨架是否完成)*

**2. 商户登录后创建商品**
```bash
curl -s -X POST http://localhost:8080/goods/add \
  -H "Content-Type: application/json" -H "Authorization: token_A" \
  -d '{"name":"测试商品1"}'
```
→ 断言:创建成功,返回商品id;查库确认该商品的 `ctId` 等于 token_A 对应的商户A的id

**2b. 请求体传入的ctId不会生效**
```bash
curl -s -X POST http://localhost:8080/goods/add \
  -H "Content-Type: application/json" -H "Authorization: token_A" \
  -d '{"name":"测试商品2","ctId":9999}'
```
→ 断言:创建成功,但查库确认该商品的 `ctId` 仍然等于商户A的id,而不是请求体里的 9999

**3. `/goods/mine` 带token查看,只看到自己名下商品**
```bash
curl -s -X GET "http://localhost:8080/goods/mine?pageNum=1&pageSize=10" -H "Authorization: token_A"
```
→ 断言:列表只包含商户A的商品,不包含其他商户的

**3b. `/goods/mine` 不带token或伪造token,拒绝访问**
```bash
curl -s -X GET "http://localhost:8080/goods/mine?pageNum=1&pageSize=10"
curl -s -X GET "http://localhost:8080/goods/mine?pageNum=1&pageSize=10" -H "Authorization: fake_token_xyz"
```
→ 断言:两次均返回**UNAUTHORIZED**(401),不返回任何商品数据

**4. `/goods/all` 不管带不带token,都能看到全部商户商品**
```bash
curl -s -X GET "http://localhost:8080/goods/all?pageNum=1&pageSize=10"
curl -s -X GET "http://localhost:8080/goods/all?pageNum=1&pageSize=10" -H "Authorization: token_A"
```
→ 断言:两次都返回全部商户的商品(不因是否携带token、携带谁的token而有任何差异),不因缺少token而报错或返回空

**4b. 商品详情不受token影响**
```bash
curl -s -X GET "http://localhost:8080/goods/detail?id=商户A的商品id"
curl -s -X GET "http://localhost:8080/goods/detail?id=商户A的商品id" -H "Authorization: token_B"
```
→ 断言:两次均成功返回商户A该商品的完整详情,不因缺少token或token属于另一商户而报错或过滤字段

**5. 商户B无法删除商户A的商品**
```bash
curl -s -X DELETE "http://localhost:8080/goods/delete?id=商户A的商品id" \
  -H "Authorization: token_B"
```
→ 断言:返回**FORBIDDEN**(403),商品未被删除

**6. 不带token或伪造token无法创建商品**
```bash
curl -s -X POST http://localhost:8080/goods/add \
  -H "Content-Type: application/json" \
  -d '{"name":"越权商品"}'

curl -s -X POST http://localhost:8080/goods/add \
  -H "Content-Type: application/json" -H "Authorization: fake_token_xyz" \
  -d '{"name":"越权商品"}'
```
→ 断言:两次都返回**UNAUTHORIZED**(401),不产生商品记录

**6b. 用户token访问商户接口,视同token不存在**
```bash
curl -s -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_user","password":"123456"}'
# 取返回的 data.token 作为 user_token

curl -s -X POST http://localhost:8080/goods/add \
  -H "Content-Type: application/json" -H "Authorization: user_token" \
  -d '{"name":"用用户token创建"}'
```
→ 断言:返回**UNAUTHORIZED**(401),不产生商品记录(证明商户接口的token校验只认`tenantToken`表,不会把合法的用户token误认成有效商户token)

**7. 商户A能修改自己的商品,商户B不能**
```bash
curl -s -X PUT http://localhost:8080/goods/update \
  -H "Content-Type: application/json" -H "Authorization: token_A" \
  -d '{"id":"商户A的商品id","name":"改名后的商品"}'

curl -s -X PUT http://localhost:8080/goods/update \
  -H "Content-Type: application/json" -H "Authorization: token_B" \
  -d '{"id":"商户A的商品id","name":"恶意改名"}'
```
→ 断言:第一条成功且名称已更新;第二条返回**FORBIDDEN**(403),名称未被篡改

**8. 重复登录使旧token失效**
```bash
# 商户A再次登录,得到 token_A2(顶替之前准备阶段拿到的 token_A)
curl -s -X POST http://localhost:8080/commercialTenant/login \
  -H "Content-Type: application/json" \
  -d '{"account":"demo_shop_a","password":"123456"}'
# 取返回的 data.token 作为 token_A2

# 用旧的 token_A 操作
curl -s -X POST http://localhost:8080/goods/add \
  -H "Content-Type: application/json" -H "Authorization: token_A" \
  -d '{"name":"用旧token创建"}'
```
→ 断言:返回**UNAUTHORIZED**(401),证明旧的 token_A 已因重复登录失效;改用新的 token_A2 操作则正常成功
