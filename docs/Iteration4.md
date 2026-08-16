## Iteration 4：商户工作台 —— 知识库管理（含向量化入库）

**目的**

把"喂知识给AI"做成工作台里的正式模块。本迭代一次性把"解析→切分→向量化→入库"做完整，不留中间状态——如果只存文本不向量化，Iteration 5一接入AI就会发现"库里没数据"，等于这一步的工作要被推倒重做。

**选型**

| 项 | 选择 | 理由 |
|---|---|---|
| 文档管理 | `GoodsDocument` 元信息表，挂载在商品详情下 | 知识库天然是商品的子资源 |
| 处理流程 | 上传时一次性完成：按扩展名解析 → 文本切分 → 向量化 → 写入向量库（metadata打上`goodsId`） | 数据从上传那一刻起就是可用状态，避免"存了文本却没进向量库"的断层 |
| 支持格式 | 限定几种常见格式（.md/.txt/.pdf），其余一律拒绝 | 控制解析器的复杂度，避免为长尾格式做兼容 |

**关键约束**

1. 商户只能操作自己商品下的文档（复用Iteration 2的归属校验）
2. 上传成功后必须立即可被检索到，不能存在"已上传但检索不到"的中间态
3. 删除文档时向量库必须同步清理，不能留下孤儿向量
4. 上传不支持的文件类型必须返回明确错误，不能抛未捕获异常导致500

**本次不做**

- AI问答消费这些向量数据（Iteration 5的事）、查询改写、rerank
- 文档版本管理/历史版本回溯
- 异步处理（本迭代是同步上传同步入库，异步化是后续独立迭代）

**验收命令**

**1. 上传文档立即可检索**
```bash
curl -s -X POST http://localhost:8080/goodsDocument/upload \
  -H "Authorization: token_A" \
  -F "file=@test-doc.md" -F "goodsId=1"
```
（文档内容含"7天无理由退货"）
→ 断言：上传成功返回文档id；随后用一个内部检索工具对`goodsId=1`检索关键词"退货"，能命中该文档内容

**2. 删除文档后向量同步清理**
```bash
curl -s -X DELETE "http://localhost:8080/goodsDocument/delete?id=上一步的文档id" \
  -H "Authorization: token_A"
```
→ 断言：删除成功；重复步骤1的检索，不再命中该文档内容

**3. 不支持的文件类型被拒绝**
```bash
curl -s -X POST http://localhost:8080/goodsDocument/upload \
  -H "Authorization: token_A" \
  -F "file=@test-doc.docx" -F "goodsId=1"
```
→ 断言：返回明确的"不支持的文件类型"错误，不是500

**4. 商户B无法删除商户A的文档**
```bash
curl -s -X DELETE "http://localhost:8080/goodsDocument/delete?id=商户A的文档id" \
  -H "Authorization: token_B"
```
→ 断言：被拒绝，文档未被删除，向量数据未受影响