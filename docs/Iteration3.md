# Iteration 3：用户端选品 + 纯人工客服实时双端通讯

**目的**

发起一次咨询的前提是用户先知道"我要问哪个商户的哪个商品"。本迭代打通"浏览商品 → 打开咨询窗口 → 发送第一句话建立会话 → 双端实时通讯 → 历史可查"这条纯人工路径，AI还未接入，所有对话默认走人工客服。"浏览门槛越低越好"这一原则只适用于商品浏览，不延伸到发起会话——发起会话需要一个可核实的身份。

**选型**

| 项 | 选择 | 理由 |
|---|---|---|
| 商品浏览 | 复用 Iteration2 已定义的 `/goods/all`，不需要登录即可浏览全部商户的商品 | Iteration2 建这个接口时就是为了给迭代3的公开浏览复用，避免重复实现分页/查询逻辑，也避免同一份"公开商品列表"在两个迭代里出现两个不同接口名 |
| **发起咨询的身份要求** | 要求用户已登录，游客只能浏览商品、不能发起会话 | 会话和历史消息需要长期归属，临时身份会导致下次回来找不回历史对话，不如直接要求登录 |
| **发起咨询的入口** | 商品详情页"咨询"按钮只是打开聊天窗口（纯前端路由跳转），此时**不**建立会话；真正建立会话的动作是用户在聊天窗口里发送第一句话，这句话通过WS连接携带 `goodsId`/`ctId` 触发会话创建 | 把"打开窗口"和"建立会话"拆成两个动作，避免用户只是点进去看一眼、还没说话就在数据库里留下一条空会话记录 |
| **会话创建成功的状态命名** | **本迭代新建 `ChatMessage` 消息体及其 `State` 枚举，直接定义为 `SUCCESS, ERROR, SESSION_CREATED, END`，不采用参考代码里语义模糊的 `SURE` 命名** | **`SURE` 语义模糊，不像`SUCCESS`/`ERROR`那样一眼看出对应什么场景；`SESSION_CREATED` 明确表达"这是一条会话创建成功的通知"，跟枚举里其它值的命名风格（描述性、动词过去分词）保持一致** |
| **`State.END` 状态的本迭代用途** | `END` 本迭代不触发任何发送场景，仅预留在枚举里供未来"会话结束"功能使用 | 本迭代不引入"会话结束"状态流转，`END` 是为未来扩展预留的枚举值 |
| **重复咨询的会话归并** | 不引入"会话结束"状态。用户发送不带 `sessionId` 的首条消息时，服务端先按 `userId+goodsId+ctId` 查询是否已有历史会话，查到就直接复用（返回已有sessionId），查不到才新建 | `Session` 的状态字段本迭代只设计 `AI/HUMAN` 两种值，不引入"结束"概念；与其现在勉强定义一套关闭规则，不如这次就按"同一用户对同一商户同一商品只维护一条会话"处理，逻辑简单且能直接验收，"会话可关闭/归档"留给以后单独的迭代定义状态流转 |
| **`Session`表结构变更** | **本迭代新建`Session`表：`id`（自增主键）、`ctId`、`userId`、`goodsId`、`conversationStatus`（枚举`AI`/`HUMAN`）、`timestamp`；需建立联合索引 `idx_user_ct_goods (user_id, ct_id, goods_id)`，`user_id`放最左** | **会话归并查询固定是"先锁定某个userId，再按ct_id+goods_id过滤"这个模式，`user_id`放联合索引最左符合最左匹配原则；这几个字段是支撑会话归属和归并查询所需的最小集合，不需要更多** |
| **会话/消息接口按RESTful改造** | **把会话与消息相关的接口统一收拢到`/session`这一个资源路径下，身份相关参数（userId/ctId）不再走query，改为从token解析。统一后的接口列表：**<br><br>**• `GET /session/user/list`** — 返回该用户的会话列表，userId从userToken解析<br>**• `GET /session/ct/list`** — 返回该商户的会话列表，ctId从tenantToken解析<br>**• `GET /session/{sessionId}/message/list`** — 查询某会话的消息记录，sessionId走路径参数<br>**• `PUT /session/{sessionId}/message/read`** — 标记消息已读，服务端凭token类型自动判断该标记哪一半为已读<br>**• `GET /session/{sessionId}/message/unread-count`** — 查询未读消息数，同样凭token类型区分 | **RESTful的核心是"URL表达资源，HTTP动词表达动作"，"谁的"这个信息该由身份（token）决定，不该塞进路径动词（`userGet.../ctGet...`）或query参数里；sessionId是被消息资源从属的路径要素，放路径参数比放query更符合"资源层级"的表达；读/未读两个原本按用户/商户拆成两条路径的接口，本质是同一个动作只是主体不同，合并后由token类型内部分流，接口数量减半也更好维护** |
| **补上消息类接口缺失的归属校验** | **`/session/{sessionId}/message/list` 与 `/session/{sessionId}/message/unread-count` 这两个新接口，必须携带有效token，且token解析出的身份（userId或ctId）必须是该session的参与方之一，否则返回FORBIDDEN(403)** | **会话消息属于私密数据，如果任何人拿到一个sessionId就能查到别人的完整聊天记录，会是一个严重的越权漏洞；设计这两个接口时必须把归属校验一并做上，判定逻辑跟约束7"查询接口只信任token身份"保持同一套标准，不新造一套规则** |
| WS 身份凭证传递方式 | 登录凭证就是 token（复用 Iteration2 的 `tenantToken`/`userToken`）。WS 握手时通过 URL query 参数携带：`/user/chat/{userId}?token=xxx`、`/commercialTenant/chat/{ctId}?token=xxx` | Jakarta WebSocket 的 `@ServerEndpoint` 注解式端点默认拿不到自定义 Authorization 请求头，只有握手阶段的 URL/Cookie 能在 `ServerEndpointConfig.Configurator` 里取到；直接放 query 参数是最小改动的落地方式，安全加固迭代再考虑更好的传递方式 |
| **WS 连接上下文封装** | **WS 连接不直接存 Jakarta `Endpoint`/`Session`，而是包一层 `ChatConnection`，内含 Jakarta `Session` 引用（用于发消息/关闭连接）、`token`（String）、`subjectId`（Long）、`subjectType`（SubjectType）。连接池按 `SubjectType` 分组存 `ChatConnection`，使连接本身脱离上下文也能自解释** | **Jakarta `Endpoint`/`Session` 是协议层对象，混入业务字段会让协议层承担业务状态的职责；`ChatConnection` 作为独立业务对象持有协议层引用，职责边界清晰；连接对象自带身份信息（`subjectType`），后续在日志、关闭、异常处理等操作中不需要依赖调用方的外部上下文推断身份类型** |
| 实时连接的身份核实与拒绝方式 | 在握手阶段（`Configurator.modifyHandshake`）校验 token 能解析出的 userId/ctId 是否与路径参数一致；不一致时在握手阶段直接拒绝，连接根本不会建立成功（onOpen 不会被触发），语义等价于 FORBIDDEN(403) | 握手阶段拒绝比"先连上、onOpen里发现不对再关闭"更干净，不会出现一个"连上又立刻断"的中间状态，也不需要额外定义WS层面的关闭码 |
| **同一身份重复建立连接** | **同一 userId（或ctId）已有一条WS连接在连接池中时，新连接握手成功后，`onOpen` 里先把池子里那条旧连接主动关闭（`close(4001, "该账号已在别处登录")`），再用新连接替换池中的引用；不是简单覆盖Map引用了事** | **连接池（`ConcurrentHashMap<Integer, Endpoint>`）是按 userId/ctId 维度存的，一个用户不管咨询多少个商品，正常情况下只应该占一个位置；但如果前端bug或多标签页导致同一身份建了两条连接，旧连接若不主动关闭，服务端会一直攥着一个业务代码找不到、也没人管的Socket，是真实的连接泄漏点，跟会话数量无关** |
| **旧连接关闭的客户端感知方式** | **主动关闭旧连接时使用WebSocket的 `close(code, reason)`，code取RFC6455私有区间的自定义值 `4001`（表示"该账号已在别处登录"）；前端在 `onclose` 回调里判断 `event.code === 4001`，走"提示用户已在别处登录"分支，而不是走默认的断线自动重连逻辑** | **如果不区分关闭原因，前端默认行为通常是"断线就自动重连"，reconnect上去立刻又被新连接顶掉，会陷入重连死循环；用私有区间关闭码显式标记"这是被顶替，不是网络问题"，前端才能做出正确的响应（弹提示，而不是重连）** |
| **心跳判活机制** | **判活使用WebSocket协议原生的 ping/pong 帧，不用业务层JSON消息：服务端每30秒发一次ping，连续2次（60秒）未收到对应pong，判定该连接已死，主动清理连接池条目、关闭连接** | **浏览器在WebSocket协议帧层（不是TCP层，是WebSocket自身的控制帧机制）收到Ping帧后会自动回复Pong帧，这一步对JS完全透明——JS既不能主动发Ping，也监听不到Pong到达，只能通过`onclose`感知连接最终断开；所以前端不需要写任何代码去处理心跳，也就不存在"前端忘了回PONG"这类bug，验证死连接时也只能靠掐断网络连接来模拟，不能指望"让前端故意不回应"。且走WebSocket协议自身的控制帧，不经过业务消息的编解码逻辑（`ChatMessageCoder`），不会把心跳流量混进业务消息流里，开销也比一条JSON消息小** |
| **token失效检查机制** | **与心跳判活完全独立：服务端起一个定时任务（如每30秒一轮），直接遍历连接池，查每条连接绑定的token是否还在`tenantToken`/`userToken`表中，不在则主动关闭该连接（同样用`close(4001, ...)`）** | **"token是否还有效"服务端自己就知道，不需要等一次ping/pong往返才能判断，没必要跟判活绑在一起；拆成两个独立机制，各自的触发条件更清晰，互不干扰，其中一个逻辑改动也不会影响另一个** |
| **会话与消息** | 会话记录选中的商品，每条消息落库，固定为人工处理模式；`conversationStatus` 字段本迭代固定写 `HUMAN`，不实现AI模式切换（留给后续迭代） | 还没有AI角色概念，所有对话默认人工处理 |
| **会话归属信息的防篡改方式** | 会话一旦建立，归属的商品和商户就固定下来，后续消息里出现的不一致信息不会生效；同理，消息里如果携带 `userId`/`ctId` 等身份字段也一律不采信，一切以WS连接建立时鉴权得到的身份为准 | 避免一段已建立的咨询关系中途被换绑到别的商品或商户，也避免消息体夹带的身份字段绕过连接层面的鉴权 |
| **消息可靠性策略** | 人工模式下消息只落 MySQL，不经过 Redis。先执行数据库insert，插入失败时立即在当前WS连接上同步回一条 `ChatMessage(state=ERROR)` 告知发送方"没发出去"；插入成功后才尝试向另一端实时转发 | Redis 目前的定位只服务于 AI 对话的短期记忆缓存，本迭代还没有AI，人工消息接入Redis没有实际用途，徒增复杂度；等后续引入AI、需要把人工历史也纳入记忆上下文时，再评估是否要给人工消息也建立Redis缓存 |
| **工作台/用户端菜单** | 沿用 Iteration2 已有的工作台骨架，本迭代不新增菜单项 | 本迭代重点是会话链路，不涉及信息架构调整 |

**关键约束**

1. 商品浏览必须公开可访问（`/goods/all`），不能被迭代2的商户身份校验波及。
2. 未登录（不带token、或携带一个在`userToken`表中查无对应记录的伪造token）尝试建立 `/user/chat/{userId}` 连接，必须在握手阶段被拒绝，不允许建立连接、不允许发起会话。
3. 会话一旦创建，归属的商品和商户信息不可通过后续消息被更改；消息体中出现的身份类字段（如夹带的 `userId`/`ctId`）同样不采信，一律以WS连接建立时鉴权得到的身份为准。
4. 商户当前不在线时，用户发送的消息仍必须成功落库（MySQL），不能因商户离线而报错或丢失；若落库本身失败，必须在当前WS连接上明确回一条 `ChatMessage(state=ERROR)` 告知发送方"没发出去"，不能让消息悄悄消失。
5. 双方都能查询到该会话的完整历史消息。
6. 建立实时连接时必须核实"声明身份"（路径参数中的userId/ctId）与"登录凭证"（token解析出的身份）一致，冒充他人身份的连接请求必须在握手阶段被拒绝，语义对应 FORBIDDEN(403)，与Iteration2的错误码体系保持一致。
7. 涉及查询归属数据的接口——历史会话查询（`GET /session/ct/list`、`GET /session/user/list`）与会话消息查询（`GET /session/{sessionId}/message/list`、`GET /session/{sessionId}/message/unread-count`）——统一遵循同一条校验规则：只信任token解析出的身份，不接受调用方通过参数指定要查询的对象。前两者不再接收userId/ctId参数，身份完全来自token；后两者除了要求携带有效token外，还需进一步确认token解析出的身份（userId或ctId）是该session的参与方之一（userId等于session.userId，或ctId等于session.ctId）。任一校验不通过，一律返回**FORBIDDEN(403)**，不能仅凭调用方传入的ID或sessionId就返回结果。
8. 已建立的WS连接需要两套独立机制共同保障：① 心跳判活（30秒一次原生ping/pong，连续2次未响应判定连接已死）；② 独立的定时任务校验连接绑定的token是否仍然有效。连接已死或token失效（如被重复登录顶替）后必须主动关闭该连接、清理连接池条目，不能让失效身份或死连接继续占用资源。
9. 同一身份（userId或ctId）同时只允许存在一条有效WS连接：新连接建立成功后，必须主动关闭该身份此前在连接池中的旧连接（使用私有关闭码4001），不能只是覆盖池中的引用而放任旧Socket空占资源；客户端必须能通过关闭码区分"被顶替"和"网络断线"，不能对被顶替的连接触发自动重连。
10. 不对"业务不活跃"（用户长时间不发消息）设置连接级别的超时踢出机制——只要连接本身通过心跳判活，就应该一直保留，用户不说话不是断开连接的理由。

**本次不做**

- AI接入、转人工判定逻辑（这个阶段还没有AI，谈不上"转"人工）
- 消息已读/未读状态、多端同时在线的连接管理（多端同时在线不做，见约束9——同一身份始终只保留最新一条连接）
- 消息推送到站外（邮件/短信通知商户）
- 游客（未登录）发起咨询——本迭代要求发起会话前必须完成登录
- 会话的"结束/关闭/归档"状态与相关流转机制——本迭代按"同一用户对同一商户同一商品只维护一条会话"处理
- 人工消息接入Redis缓存——留给后续AI迭代按需评估
- 基于"业务不活跃"的连接超时踢出——本迭代只做心跳判活清理死连接，不判断用户是否长时间没说话

**验收命令**

**1. 未登录可浏览全部商品**
```bash
curl -s "http://localhost:8080/goods/all?pageNum=1&pageSize=10"
```
→ 断言：返回全部商户的商品列表，无需任何身份凭证

**2. 未登录无法建立用户端WS连接**
```bash
# 不带token，或带一个伪造token
# WS连接 ws://localhost:8080/user/chat/1
# WS连接 ws://localhost:8080/user/chat/1?token=fake_token_xyz
```
→ 断言：两次连接均在握手阶段被拒绝，无法建立连接

**3. 用户发送首条消息建立会话**
```bash
# WS连接 ws://localhost:8080/user/chat/{userId}?token=user_token_1
# 发送: {"goodsId": 1, "ctId": 1, "message": "你好"}
```
→ 断言：收到 `state=SESSION_CREATED` 的响应，携带新生成的会话标识，服务端记录该会话为人工处理模式

**4. 归属信息不可被后续消息篡改**
```bash
# 同一会话标识下发送: {"sessionId": "上一步的id", "goodsId": 999, "message": "测试篡改"}
```
→ 断言：该会话归属的商品信息仍是最初创建时的值，未被改成999

**5. 商户实时收到并回复**
```bash
# 商户WS连接 ws://localhost:8080/commercialTenant/chat/{ctId}?token=token_A
# 断言：能收到步骤3用户发送的消息推送
# 商户回复: {"sessionId": "...", "message": "您好，有什么可以帮您"}
```
→ 断言：用户端能实时收到商户的回复

**6. 商户离线时消息不丢，且不经过Redis**
```bash
# 不建立商户WS连接，用户直接发消息
curl -s "http://localhost:8080/session/xxx/message/list" -H "Authorization: user_token_1"
```
→ 断言：消息已正常写入MySQL，可查询到，不因商户离线而报错

**7. 历史记录双向可查**
```bash
curl -s "http://localhost:8080/session/ct/list" -H "Authorization: token_A"
curl -s "http://localhost:8080/session/user/list" -H "Authorization: user_token_1"
```
→ 断言：商户和用户各自都能在自己的会话列表里看到这个会话（userId/ctId均从token解析，接口不再接收这两个参数），且能拉到完整历史消息

**8. 冒充他人身份建立连接必须被拒绝**
```bash
# 用商户A的token_A，尝试连接 ws://localhost:8080/commercialTenant/chat/{ctId_B}?token=token_A
```
→ 断言：握手阶段被拒绝，连接无法建立，不能以商户B的身份收发消息

**9. 历史查询越权必须被拒绝**
```bash
curl -s "http://localhost:8080/session/ct/list" -H "Authorization: token_A"
```
→ 断言：`/session/ct/list` 不接收ctId参数，只能查token_A自己名下（商户A，ctId=1）的会话列表；返回结果里不会出现商户2的会话数据，无论token_A是否尝试携带其它ctId

**10. 消息接口越权查询必须被拒绝**
```bash
# 商户A的商品对应的会话sessionId=1，商户B用自己的token_B去查
curl -s "http://localhost:8080/session/1/message/list" -H "Authorization: token_B"
curl -s "http://localhost:8080/session/1/message/unread-count" -H "Authorization: token_B"
```
→ 断言：token_B对应的商户B不是session=1的参与方，两条均返回**FORBIDDEN(403)**，不返回该会话的消息内容或未读数

**11. 重复咨询复用同一会话**
```bash
# 用户对goodsId=1,ctId=1第一次发起会话，得到sessionId_1
# 断开重连，再次对goodsId=1,ctId=1发起会话（不带sessionId）
```
→ 断言：第二次返回的会话标识与sessionId_1相同，未创建新会话

**12. 重复登录使已建立的WS连接失效**
```bash
# 用户已用user_token_1建立WS连接
# 用同一账号再次登录，得到 user_token_2（顶替user_token_1）
# 等待一轮token校验定时任务（约30秒）
```
→ 断言：使用user_token_1建立的旧连接被服务端主动关闭（关闭码4001），客户端`onclose`能读到该码而不是触发自动重连；改用user_token_2重新连接则正常

**13. 落库失败时明确告知发送方**
```bash
# 模拟MySQL写入失败场景（如断开数据库连接后发送消息）
```
→ 断言：发送方在当前WS连接上收到 `state=ERROR` 的响应，消息内容明确提示未发送成功，不是消息悄悄消失

**14. 同一用户重复建立连接，旧连接被主动关闭**
```bash
# 用户用user_token_1建立第一条WS连接（连接1）
# 同一用户、同一token，再建立第二条WS连接（连接2）
```
→ 断言：连接2建立成功；连接1被服务端主动关闭（关闭码4001，而不是仍然挂着但收不到任何消息），此后向"用户1"发消息只会送达连接2

**15. 心跳判活（原生ping/pong），连续未响应的死连接被主动清理**
```bash
# 建立WS连接后，模拟客户端不再响应服务端的原生ping帧（如断网但不主动关闭Socket）
# 等待约2个心跳周期（60秒左右）
```
→ 断言：服务端判定该连接已死，主动从连接池中移除；此后向该身份发消息不会再尝试推送到这条死连接

**16. 用户长时间不发消息，连接不会被主动断开**
```bash
# 建立WS连接后，正常响应心跳，但不发送任何业务消息，等待超过5分钟
```
→ 断言：连接始终保持存活，未被服务端以"不活跃"为由主动关闭