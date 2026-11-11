# 第10章 风险与应对

| 编号 | 风险 | 概率 | 影响 | 应对 |
| --- | --- | --- | --- | --- |
| R1 | 冷启动录入负担过重，用户第一次就放弃 | 高 | 高 | 3 张起录；关系链选择代替手填；相册批量导入；家族共建；把“录入”做成拜年前的 10 分钟任务 |
| R2 | 用户把远距离“发现”误解为“识别” | 高 | 中 | 远距界面只显示“有人/未知”，不显示姓名；产品话术区分“发现”与“认出”；近距才显示称呼 |
| R3 | 老人、儿童、跨年份识别率低 | 中 | 高 | 多模板；质量门限；拒识而非误认；手动兜底；每年补充照片 |
| R4 | 长相相似的亲属被误认 | 中 | 高 | 开集 + margin；候选列表；每人自适应阈值；纠正样本回填 |
| R5 | 模型/权重许可证踩坑 | 高 | 高 | 立项即定许可路线；优先 Apache/MIT；自训或购买授权；法务复核 |
| R6 | 隐私舆情（“这不是偷拍识别吗”） | 中 | 高 | 纯本地、无上传、无广告 SDK；显著告知；不识别陌生人；公开技术白皮书 |
| R7 | ColorOS 14–17 版本差异/ROM 限制 | 高 | 中 | 四档机型矩阵；前台服务/电池/自启动引导；GPU 回退；老机型降级 |
| R8 | 连续使用发热/掉电 | 中 | 中 | 动态帧率；识别成功降频；省电模式；热状态监测 |
| R9 | 称呼歧义、方言差异导致“叫错” | 高 | 中 | 候选列表；地域设置；自定义别名；“为什么这样叫”可解释 |
| R10 | 关系图补全错误，张冠李戴 | 中 | 高 | 置信度分级；低置信度只显示“可能”；用户确认后才落库 |
| R11 | 换机/误删导致数据丢失 | 中 | 中 | 加密导出/导入；最近删除；提示备份 |
| R12 | 进度延期 | 中 | 中 | MVP 范围冻结；先跑通“远距检测/跟踪 + 近距识别”单链路；家族共建延后 |
| R13 | 只适配 ColorOS，用户覆盖受限 | 高 | 中 | 作为主动取舍；先把 ColorOS 14–17 做深；后续扩展其他 ROM 需重做机型矩阵 |
| R14 | ColorOS 17/Android 17 行为变更 | 中 | 中 | targetSdk 37 回归；重点测相机、前台服务、权限、16 KB 页 |

## 10.1 三个“一票否决”项

1. **许可证**：如果产品要闭源商用，却直接打包了 InsightFace 权重或 YOLO，必须在发布前替换，否则可能面临 AGPL 强制开源或侵权索赔。
2. **隐私**：如果为了“体验好”偷偷上传人脸或接入统计 SDK，一旦被抓包，产品信任直接归零，且可能违反《人脸识别技术应用安全管理办法》第 8 条。
3. **误认**：如果把“未知”强行匹配成某个亲戚，用户叫错人的尴尬会直接导致卸载。宁可拒识，不可误认。

# 第11章 实施路线图与成本

## 11.1 MVP 范围

| 优先级 | 功能 | 验收 |
| --- | --- | --- |
| P0 | 远距模式：人体检测 + ByteTrack，不加载人脸模型 | 10–20 m 人体召回 ≥ 80%；中端 FAR ≥ 25 FPS |
| P0 | 近距模式：人脸检测 + 质量 + 特征 + 1:N | ≤4 m 可识别；30 人 rank-1 ≥ 95% |
| P0 | 模式切换（65%/55% 滞回） | 边界不闪烁；切换 ≤ 200 ms |
| P0 | 相机实时预览 + 本地人脸库（增删改查）+ 加密 | 30 人库，重启不丢 |
| P0 | 拍照/相册录入 + 关系链选择 | 1 人 ≤ 60 秒 |
| P0 | 实时显示称呼 + 候选 + 手动纠正 | 3 帧内稳定 |
| P0 | 称呼计算（relationship.js 移植） | 50 条标准用例全过 |
| P0 | ColorOS 14–17 四档机型跑通 | 每档至少 1 台，共 6–8 台 |
| P1 | 关系图推理 + 置信度 + 确认 | 20 条推理用例 |
| P1 | TTS 播报 | 中文离线可用 |
| P2 | 家族共建/加密导出 | 两台设备互通 |
| P2 | 静默活体 | 照片攻击拦截率 ≥ 90% |
| P2 | 方言/地域变体 | 覆盖 5 种主要方言 |

## 11.2 阶段计划（12 周）

| 阶段 | 周期 | 交付 | 关键风险 |
| --- | --- | --- | --- |
| 技术验证 | 第 1–2 周 | ColorOS 14/17 各 1 台；远距人体检测 + ByteTrack Demo；近距人脸/特征 Demo；10 人小库 | 模型转换、GPU delegate、CameraX 兼容 |
| MVP-1 | 第 3–5 周 | 录入向导、本地库、近距识别、称呼显示 | 录入体验、阈值 |
| MVP-2 | 第 6–8 周 | 远距检测/跟踪、模式切换、ColorOS 15/16 适配 | 跟踪稳定性、切换抖动 |
| 关系推理 | 第 9–10 周 | 关系图、BFS、规则补全、方言 | 称谓歧义、数据模型 |
| 内测与合规 | 第 11–12 周 | ColorOS 14–17 共 6–8 台内测、性能调优、隐私政策/PIPIA、上架材料 | 真实场景准确率、合规 |

## 11.3 团队配置

| 角色 | 人数 | 职责 |
| --- | --- | --- |
| Android 工程师 | 1–2 | CameraX、UI、数据层、集成 |
| CV/算法工程师 | 1 | 模型转换、量化、跟踪、阈值校准 |
| 产品/设计 | 0.5 | 录入流程、信息架构、隐私体验 |
| 测试 | 0.5 | 设备矩阵、真实场景用例 |
| 法务/合规 | 按需 | 隐私政策、PIPIA、许可复核 |

小团队可以 2 人完成：1 人 Android + 1 人算法，产品/设计兼职，周期拉到 14–16 周。

## 11.4 成本估算

| 项目 | 自研（万元） | 说明 |
| --- | --- | --- |
| 人力（7–9 人月） | 25–45 | 按 3–5 万元/人月 |
| 设计/测试 | 3–8 | 兼职或外包 |
| 设备（6–8 台） | 2–4 | ColorOS 14/15/16/17 各 1–2 台，覆盖骁龙/天玑 |
| 合规/法务 | 2–5 | 隐私政策、PIPIA、许可复核 |
| 模型授权（可选） | 10–50 | 若不自训、需商用授权 |
| **合计** | **40–100** | 不含推广/服务器（本项目无服务器） |

个人/学生项目：现金成本可压到 0–3 万元（设备自备、模型用研究许可、自己标注），主要成本是 3–6 个月时间。

## 11.5 验收指标

| 指标 | 目标 | 测试方法 |
| --- | --- | --- |
| 近距识别 rank-1 | ≥ 95% | 30 人 × 5 底照，近距 2–3 m |
| 4 m 边界识别 | 可出候选，不强制接受 | 4 m 距离测试 |
| 误认率 FAR | ≤ 1% | 1000 对负样本 |
| 拒识率 FRR | ≤ 10% | 同上 |
| 远距人体召回 | ≥ 80% @10–20 m | 20 段视频，标注人体框 |
| 跟踪 IDS | < 0.5 次/分钟 | 5 人 3 分钟视频 |
| 远距端到端延迟 | p95 ≤ 100 ms | ColorOS 15–16，Perfetto |
| 近距端到端延迟 | p95 ≤ 150 ms | 同上 |
| 模式切换 | 65%/55% 不抖动 | 人走进/走出 10 次 |
| 录入时长 | ≤ 60 秒/人 | 5 名用户实测 |
| 连续使用温升 | ≤ 8 °C | 10 分钟，热像仪 |
| 电量消耗 | ≤ 15%/10 分钟 | Battery Historian |
| 设备矩阵 | ColorOS 14–17 每档通过 | 6–8 台真机 |
| 崩溃率 | < 0.5% | 内测统计 |

## 11.6 上线后的迭代

- **V1.1**：家族共建、二维码分享、方言包、TTS 音色；
- **V1.2**：2× 长焦/4K 裁切、多人同时识别、ColorOS 17 新特性适配；
- **V2.0**：端侧小模型解析自然语言录入（“这是我爸的二哥”）、跨设备加密同步（需重新做合规评估）；评估扩展到其他 Android ROM（需重做机型矩阵）。

# 第12章 结论与建议

## 12.1 结论

1. **技术可行。** 端侧人体检测、多目标跟踪、人脸检测/特征提取、称呼推理均有成熟方案；ColorOS 14–17 的中端机可做到远距 25–30 FPS、近距 15–20 FPS。
2. **产品成败不在算法，在“关系”和“录入”。** 人脸只解决“是谁”，称呼需要“我”的关系图；用户是否愿意录入 20–50 位亲戚，决定产品能不能用起来。
3. **远距离只做“发现与跟踪”，近距离才做“识别”。** 1080p 下 2–3 m 稳定识别、4 m 出候选；更远只能检测/跟踪。这是光学决定的，不是换个模型就能解决。
4. **合规是底线，不是加分项。** 本地存储、单独同意、未成年人保护、可撤回可删除、PIPIA，缺一不可。
5. **许可证要先行。** YOLO 的 AGPL、InsightFace 权重的非商用、DeepSORT 的 GPL，是三个必须提前决策的点。
6. **只适配 ColorOS 14–17 是主动聚焦。** 好处是机型矩阵收敛到 4 个系统大版本、测试成本可控；代价是用户覆盖变窄，后续扩展其他 ROM 需要重做适配与回归。

## 12.2 六条建议

1. **先做“关系图 + 称呼”，再做人脸。** 用 1 周做一个纯手动的关系推理 Demo，找 10 个亲戚测试“能不能算出正确称呼”；如果这一步不成立，人脸做得再好也没用。
2. **先用 LiteRT 跑通“远距检测/跟踪”，再接“近距识别”。** 远距链路简单、风险低，能最快验证“有人来了”的价值；近距识别后接。
3. **把录入做成“拜年前 10 分钟”的轻任务。** 3 张照片 + 一个关系链选择；支持相册批量；支持家族共建。
4. **默认拒识，候选优先。** 显示“可能是三姨/四姨”，比错误地叫“三姨”更安全。
5. **先锁定 ColorOS 14–17 的 6–8 台设备。** 每个大版本至少 1 台，覆盖骁龙/天玑；不要等开发完再找机器。
6. **从第一天就按合规设计。** 不申请多余权限、不接统计 SDK、不开启系统备份、不上传任何特征。

## 12.3 未来两周的验证计划

| 周 | 任务 | 产出 | 通过标准 |
| --- | --- | --- | --- |
| W1 | 远距：CameraX + EfficientDet + ByteTrack，ColorOS 14/17 | 可运行 Demo | 10–20 m 人体召回 ≥ 80%，≥ 25 FPS |
| W1 | 近距：SCRFD + MobileFaceNet，10 人小库，ColorOS 15/16 | 可运行 Demo | 2–3 m rank-1 ≥ 90% |
| W2 | 4 m 边界/光照/遮挡 + 65%/55% 模式切换 | 测试报告 | 边界可出候选，切换不抖动 |
| W2 | 关系链录入 + relationship.js 移植 | 称呼计算 Demo | 50 条用例 ≥ 95% 正确 |
| W2 | 20–30 人真实家庭数据采集（经同意） | 测试集 + 阈值 | FAR ≤ 1% |
| W2 | 许可与合规评审 | 决策记录 | 确定开源/商用路线 |

**止损条件：** 如果 2–3 m 内 rank-1 < 85%，或 10–20 m 人体召回 < 70%，或单人录入 > 90 秒，则应调整方向：先做“相册 + 手动关系图”的轻量版，而不是继续堆实时识别。

## 12.4 最后的话

“阿姨你好”是一个有真实情感价值的工具。它解决的不是“识别”问题，而是“人情”问题：让年轻人少一点尴尬，让长辈多一点被记住的感觉。技术方案可以很复杂，但产品体验必须很简单——抬起手机，看到称呼，叫对人。

如果只能记住一句话：**这不是一个人脸识别 App，这是一个“家庭关系助手”，人脸只是它的眼睛。**

# 附录 A 常见亲属称呼对照表

> 说明：下表以“我”为起点；同一称呼在不同地区差异很大，表中列出常见变体，产品应以用户设置为准。

## A.1 父系

| 关系链 | 标准称呼 | 常见变体 | 备注 |
| --- | --- | --- | --- |
| 爸爸的爸爸 | 爷爷 | 祖父、阿公 | — |
| 爸爸的妈妈 | 奶奶 | 祖母、阿婆 | — |
| 爸爸的哥哥 | 伯父 | 大伯、伯伯 | 按排行称大伯/二伯 |
| 爸爸的弟弟 | 叔叔 | 叔父、小叔 | 按排行称二叔/三叔 |
| 爸爸的姐妹 | 姑姑 | 姑妈、娘娘、姑姐 | 按排行称大姑/二姑 |
| 爸爸的哥哥的妻子 | 伯母 | 大娘、伯娘 | — |
| 爸爸的弟弟的妻子 | 婶婶 | 婶娘、婶母 | — |
| 爸爸的姐妹的丈夫 | 姑父 | 姑丈 | — |
| 爸爸的哥哥的儿子 | 堂哥/堂弟 | 堂兄 | 按与“我”的年龄 |
| 爸爸的哥哥的女儿 | 堂姐/堂妹 | 堂姊 | 按与“我”的年龄 |
| 爸爸的姐妹的儿子 | 表哥/表弟 | 表兄 | 按与“我”的年龄 |
| 爸爸的姐妹的女儿 | 表姐/表妹 | 表姊 | 按与“我”的年龄 |
| 爷爷的哥哥 | 伯公 | 大爷爷、伯祖父 | 地域差异大 |
| 爷爷的弟弟 | 叔公 | 小爷爷、叔祖父 | 地域差异大 |
| 爷爷的姐妹 | 姑婆 | 姑奶奶 | — |
| 奶奶的兄弟 | 舅公 | 舅爷爷 | — |
| 奶奶的姐妹 | 姨婆 | 姨奶奶 | — |

## A.2 母系

| 关系链 | 标准称呼 | 常见变体 | 备注 |
| --- | --- | --- | --- |
| 妈妈的爸爸 | 外公 | 姥爷、阿公 | 北方多称姥爷 |
| 妈妈的妈妈 | 外婆 | 姥姥、阿婆、家婆 | 北方多称姥姥 |
| 妈妈的哥哥/弟弟 | 舅舅 | 舅父 | 按排行称大舅/二舅 |
| 妈妈的姐妹 | 姨妈 | 阿姨、姨姨 | 按排行称大姨/二姨 |
| 舅舅的妻子 | 舅妈 | 舅母、妗子 | — |
| 姨妈的丈夫 | 姨父 | 姨丈 | — |
| 舅舅的儿子/女儿 | 表哥/表弟、表姐/表妹 | 表兄、表姊 | 按年龄 |
| 姨妈的子女 | 表哥/表弟、表姐/表妹 | — | 按年龄 |
| 外公的哥哥 | 伯外公 | 外伯公 | 地域差异大 |
| 外公的弟弟 | 叔外公 | 外叔公 | 地域差异大 |
| 外婆的姐妹 | 姨婆 | 姨姥姥 | — |

## A.3 同辈与姻亲

| 关系链 | 标准称呼 | 常见变体 | 备注 |
| --- | --- | --- | --- |
| 哥哥的妻子 | 嫂子 | 嫂嫂 | — |
| 弟弟的妻子 | 弟媳 | 弟妹 | — |
| 姐姐的丈夫 | 姐夫 | — | — |
| 妹妹的丈夫 | 妹夫 | — | — |
| 妻子的父亲 | 岳父 | 丈人、老丈人 | — |
| 妻子的母亲 | 岳母 | 丈母娘 | — |
| 丈夫的父亲 | 公公 | 家公 | — |
| 丈夫的母亲 | 婆婆 | 家婆 | — |
| 丈夫的哥哥 | 大伯子 | 大伯哥 | 背称 |
| 丈夫的弟弟 | 小叔子 | — | 背称 |
| 丈夫的姐姐 | 大姑子 | — | 背称 |
| 丈夫的妹妹 | 小姑子 | — | 背称 |
| 妻子的哥哥 | 大舅子 | — | 背称 |
| 妻子的弟弟 | 小舅子 | — | 背称 |
| 妻子的姐姐 | 大姨子 | — | 背称 |
| 妻子的妹妹 | 小姨子 | — | 背称 |
| 姐妹的丈夫互称 | 连襟 | 襟兄弟 | — |
| 兄弟的妻子互称 | 妯娌 | — | — |

## A.4 晚辈与干亲

| 关系链 | 标准称呼 | 常见变体 | 备注 |
| --- | --- | --- | --- |
| 兄弟的儿子 | 侄子 | 侄儿 | — |
| 兄弟的女儿 | 侄女 | — | — |
| 姐妹的儿子 | 外甥 | — | — |
| 姐妹的女儿 | 外甥女 | — | — |
| 儿子的妻子 | 儿媳 | 媳妇 | — |
| 女儿的丈夫 | 女婿 | — | — |
| 儿子的儿子 | 孙子 | — | — |
| 女儿的儿子 | 外孙 | — | — |
| 干爹/干妈 | 干爹/干妈 | 义父/义母 | 需标注“干亲” |
| 继父/继母 | 继父/继母 | — | 需标注“继亲” |
| 养父/养母 | 养父/养母 | — | 需标注“收养” |

# 附录 B 核心伪代码

## B.1 实时管线主循环

```kotlin
fun onFrame(image: ImageProxy) {
    val frame = preprocess(image)                 // YUV -> RGB, resize, ROI
    val persons = personDetector.detect(frame)    // 8-10 Hz
    val tracks = tracker.update(persons)          // every frame

    tracks.forEach { track ->
        when (track.state) {
            FAR -> {
                // 远距：只跑人体检测 + 跟踪，不调用人脸模型
                if (track.heightRatio >= 0.65) track.state = NEAR
                ui.showUnknown(track)
            }
            NEAR, IDENTIFIED -> {
                val faces = faceDetector.detect(frame, track.roi)
                faces.filter { quality(it) >= MIN_QUALITY }.forEach { face ->
                    val emb = embedder.embed(frame, face)
                    if (emb != null) {
                        val m = matcher.match(emb)
                        track.vote(m)
                    }
                }
                ui.show(track.bestMatch())
            }
            LOST -> ui.fadeOut(track)
        }
    }
    image.close()
}
```

## B.2 开集匹配

```kotlin
fun match(q: FloatArray): MatchResult {
    var top1 = -1f; var top2 = -1f; var id = -1L
    for (p in gallery) {
        val s = cosine(q, p.embedding)
        if (s > top1) { top2 = top1; top1 = s; id = p.personId }
        else if (s > top2) { top2 = s }
    }
    val accepted = top1 >= threshold(id) && (top1 - top2) >= MARGIN
    return MatchResult(id, top1, top2, accepted)
}
```

## B.3 关系路径 BFS

```kotlin
fun shortestPath(ego: Long, target: Long): List<Edge>? {
    val queue = ArrayDeque<Long>().apply { add(ego) }
    val prev = HashMap<Long, Edge>()
    val visited = HashSet<Long>().apply { add(ego) }
    while (queue.isNotEmpty()) {
        val cur = queue.removeFirst()
        if (cur == target) break
        for (e in graph.edgesOf(cur)) {
            val next = e.other(cur)
            if (next !in visited) {
                visited.add(next); prev[next] = e; queue.add(next)
            }
        }
    }
    if (target !in prev) return null
    return buildPath(prev, ego, target)
}
```

## B.4 关系补全规则（示例）

```text
IF parent(A, B) AND parent(A, C) AND B != C
THEN sibling(B, C) WITH confidence 0.9

IF spouse(A, B) AND parent(A, C)
THEN parent(B, C) WITH confidence 0.6   // 可能是继母/继父，需确认

IF sibling(A, B) AND parent(A, C)
THEN parent(C, B) WITH confidence 0.6   // 同父异母时不一定
```

## B.5 称呼查询

```kotlin
fun term(ego: Long, target: Long): TermResult {
    val path = shortestPath(ego, target) ?: return TermResult.unknown()
    val selector = path.toSelector()        // e.g. "爸爸的哥哥的妻子"
    val candidates = relationship.query(selector, region)
    return TermResult(candidates, path, confidence = path.minConfidence())
}
```

# 附录 C 参考资料

## C.1 法律与监管

1. 国家互联网信息办公室、公安部：《人脸识别技术应用安全管理办法》，2025 年 6 月 1 日施行。https://www.gov.cn/zhengce/zhengceku/202503/content_7016075.htm
2. 国家互联网信息办公室：《关于开展人脸识别技术应用备案工作的公告》，2025 年 5 月 28 日。https://www.hunan.gov.cn/zqt/zcsd/202506/t20250603_33689743.html
3. 《中华人民共和国个人信息保护法》，2021 年 11 月 1 日施行。https://www.gov.cn/xinwen/2021-08/20/content_5632486.htm
4. 《最高人民法院关于审理使用人脸识别技术处理个人信息相关民事案件适用法律若干问题的规定》，法释〔2021〕15 号，2021 年 8 月 1 日施行。
5. 《中华人民共和国数据安全法》《中华人民共和国网络安全法》《中华人民共和国未成年人保护法》。

## C.2 模型与论文

6. Deng J., Guo J., et al. ArcFace: Additive Angular Margin Loss for Deep Face Recognition. CVPR 2019. https://arxiv.org/abs/1801.07698
7. Guo J., Deng J., et al. Sample and Computation Redistribution for Efficient Face Detection (SCRFD). ICLR 2022. https://openreview.net/forum?id=RhB1AdoFfGE
8. Chen S., Liu Y., et al. MobileFaceNets: Efficient CNNs for Accurate Real-Time Face Verification on Mobile Devices. 2018. https://arxiv.org/abs/1804.07573
9. Zhang K., Zhang Z., et al. Joint Face Detection and Alignment Using Multitask Cascaded Convolutional Networks. IEEE SPL 2016.
10. Zhang Y., Sun P., et al. ByteTrack: Multi-Object Tracking by Associating Every Detection Box. ECCV 2022. https://github.com/FoundationVision/ByteTrack
11. Cao J., Pang J., et al. OC-SORT: Observation-Centric SORT. CVPR 2023. https://github.com/noahcao/OC_SORT
12. Aharon N., Orfaig R., Bobrovsky B.-Z. BoT-SORT. 2022. https://github.com/NirAharon/BoT-SORT
13. Wojke N., Bewley A., Paulus D. Simple Online and Realtime Tracking with a Deep Association Metric (DeepSORT). ICIP 2017.

## C.3 工程与开源

14. Google AI Edge, MediaPipe Face Detector. https://ai.google.dev/edge/mediapipe/solutions/vision/face_detector
15. Google AI Edge, LiteRT. https://ai.google.dev/edge/litert
16. Android Developers, ML Kit Face Detection. https://developers.google.com/ml-kit/vision/face-detection/android
17. Android Developers, NNAPI Migration Guide. https://developer.android.com/ndk/guides/neuralnetworks/migration-guide
18. Android Developers, CameraX. https://developer.android.com/media/camera/camerax
19. InsightFace Model Zoo. https://github.com/deepinsight/insightface/tree/master/model_zoo
20. Ultralytics License（AGPL-3.0 与商业授权）. https://www.ultralytics.com/license
21. mumuy/relationship（中国亲戚关系计算器，MIT）. https://github.com/mumuy/relationship
22. ObjectBox On-device Vector Search. https://docs.objectbox.io/on-device-vector-search
23. SQLCipher. https://www.zetetic.net/sqlcipher/
24. Android Keystore. https://developer.android.com/privacy-and-security/keystore
25. Android Developers：设置 Android 17 SDK. https://developer.android.com/about/versions/17/setup-sdk
26. Android Developers：16 KB 页大小兼容性. https://developer.android.com/guide/practices/page-sizes
27. Android Developers：前台服务类型（camera）. https://developer.android.com/develop/background-work/services/fgs/service-types
28. ColorOS 17（基于 Android 17）发布信息. https://www.digit.in/news/general/coloros-17-ai-features-release-timeline-eligible-oneplus-oppo-and-realme-phones-and-more-details.html

> 说明：以上链接为调研期间（2026 年 11 月）可访问的公开资料；模型版本、许可证与价格可能变化，集成前请复核仓库 LICENSE 与官方文档。

# 附录 D 术语表

| 术语 | 含义 |
| --- | --- |
| 人脸检测（Face Detection） | 在图像中定位人脸框与关键点，不判断是谁 |
| 人脸识别（Face Recognition） | 1:N 比对，判断“是谁”；本报告中的“识别”多指此意 |
| 人脸验证（Face Verification） | 1:1 比对，判断“是不是同一个人” |
| Embedding | 人脸特征向量，通常 128/512 维，经 L2 归一化 |
| 开集识别（Open-set） | 库外的人也能正确拒识，不强行归类 |
| Rank-1 | 检索结果第一位命中的比例 |
| FAR / FRR | 误认率 / 拒识率 |
| NMS | 非极大值抑制，合并重叠检测框 |
| INT8 量化 | 用 8 位整数表示权重/激活，减小体积、提升速度 |
| ByteTrack | 基于 IoU + Kalman 的多目标跟踪算法 |
| IDS | 身份切换次数，跟踪稳定性指标 |
| Ego | 关系图中的“我”，称呼计算的起点 |
| 堂/表 | 父系兄弟的子女为“堂”，其余（父系姐妹、母系兄弟姐妹）的子女为“表” |
| 排行 | 同辈中的长幼顺序，如大伯/二伯、大姨/小姨 |
| PIPIA | 个人信息保护影响评估 |
| ColorOS | OPPO/一加/真我基于 Android 定制的系统；本报告只适配 14–17 |
| API level | Android 14=34、15=35、16=36、17=37 |
| 16 KB page size | Android 15+ 对 native 库页对齐的要求，影响 LiteRT/NCNN 的 `.so` |
