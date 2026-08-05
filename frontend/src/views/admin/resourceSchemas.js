// 资源 schema 配置：驱动侧边栏菜单、表格列与编辑表单
// type 取值: text / textarea / markdown / number / boolean / tags / date / datetime / image
// columns 为表格展示列（3-5 个代表字段）；wide 表示编辑用宽抽屉
// image 字段可配 ratio（宽/高）：站内图裁切时按该比例锁定，与前台显示比例对齐
// batch: true 的字段参与列表多选后的批量修改（select/boolean 下拉，number/text 弹窗输入）

export const resourceSchemas = [
  {
    key: 'articles',
    label: '文章管理',
    wide: true,
    columns: ['id', 'title', 'archiveCategory', 'publishedAt', 'readingTime'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'title', label: '标题', type: 'text' },
      { name: 'summary', label: '摘要', type: 'textarea' },
      { name: 'content', label: '正文（Markdown）', type: 'markdown' },
      { name: 'archiveCategory', label: '归档分类', type: 'text' },
      { name: 'tags', label: '标签', type: 'tags' },
      { name: 'status', label: '状态（已发布/草稿）', type: 'text' },
      { name: 'pinned', label: '置顶', type: 'boolean' },
      { name: 'publishedAt', label: '发布时间', type: 'datetime' },
      { name: 'readingTime', label: '阅读时长', type: 'text' },
      { name: 'mood', label: '心情语', type: 'textarea' },
      { name: 'coverUrl', label: '封面图', type: 'image' },
      { name: 'categoryId', label: '栏目 ID', type: 'number' },
      { name: 'categoryName', label: '栏目名', type: 'text' },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'home-carousels',
    label: '首页轮播',
    columns: ['id', 'title', 'sortIndex', 'imageUrl'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'title', label: '标题', type: 'text' },
      { name: 'description', label: '描述', type: 'textarea' },
      { name: 'content', label: '内容', type: 'textarea' },
      { name: 'imageUrl', label: '图片', type: 'image', ratio: 16 / 10 },
      { name: 'sortIndex', label: '排序值', type: 'number' },
      { name: 'sceneLabel', label: '编号标签（如 SCENE 01）', type: 'text' },
      { name: 'kicker', label: '眉标 Kicker（如 PERSPECTIVE）', type: 'text' },
      { name: 'badge', label: '角标 Badge（如 04/19）', type: 'text' },
      { name: 'visible', label: '在首页显示', type: 'boolean', batch: true },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'collapse-cards',
    label: '折叠卡片',
    columns: ['id', 'title', 'sortIndex', 'imageUrl'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'title', label: '标题', type: 'text' },
      { name: 'description', label: '描述', type: 'textarea' },
      { name: 'content', label: '内容', type: 'textarea' },
      { name: 'imageUrl', label: '图片', type: 'image' },
      { name: 'sortIndex', label: '排序值', type: 'number' },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'team-members',
    label: '团队成员',
    columns: ['id', 'displayName', 'roleLabel', 'position', 'email'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'displayName', label: '昵称', type: 'text' },
      { name: 'email', label: '邮箱', type: 'text' },
      { name: 'avatarUrl', label: '头像', type: 'image', ratio: 1 },
      { name: 'roleCode', label: '角色编码', type: 'text' },
      { name: 'roleLabel', label: '角色名', type: 'text' },
      { name: 'position', label: '职位', type: 'text' },
      { name: 'description', label: '介绍', type: 'textarea' },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'archive-categories',
    label: '归档分类',
    columns: ['id', 'category', 'title', 'tags'],
    fields: [
      { name: 'category', label: '分类标识', type: 'text' },
      { name: 'title', label: '标题', type: 'text' },
      { name: 'description', label: '描述', type: 'textarea' },
      { name: 'tags', label: '标签', type: 'tags' }
    ]
  },
  {
    key: 'timeline-carousels',
    label: '时间线轮播',
    columns: ['id', 'title', 'imageUrl', 'updatedAt'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'title', label: '标题', type: 'text' },
      { name: 'content', label: '内容', type: 'textarea' },
      { name: 'imageUrl', label: '图片', type: 'image' },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'timeline-events',
    label: '时间线事件',
    columns: ['id', 'title', 'timelineDate', 'imageUrl'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'title', label: '标题', type: 'text' },
      { name: 'content', label: '内容', type: 'textarea' },
      { name: 'imageUrl', label: '图片', type: 'image' },
      { name: 'timelineDate', label: '事件日期', type: 'date' },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'parallax-stories',
    label: '视差故事',
    columns: ['id', 'title', 'imageUrl', 'align', 'sortIndex'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'title', label: '标题', type: 'text' },
      { name: 'description', label: '描述', type: 'textarea' },
      { name: 'note', label: '旁白', type: 'textarea' },
      { name: 'align', label: '对齐（left/bottom/right）', type: 'text' },
      { name: 'imageUrl', label: '图片', type: 'image' },
      { name: 'sortIndex', label: '排序值', type: 'number' }
    ]
  },
  {
    key: 'tool-sites',
    label: '工具站点',
    columns: ['id', 'websiteName', 'category', 'featured', 'websiteUrl'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'websiteName', label: '站点名', type: 'text' },
      { name: 'websiteDescription', label: '站点描述', type: 'textarea' },
      { name: 'websiteUrl', label: '站点地址', type: 'text' },
      { name: 'categoryId', label: '分类 ID', type: 'number' },
      { name: 'category', label: '分类名', type: 'text' },
      { name: 'iconUrl', label: '图标', type: 'image', ratio: 1 },
      { name: 'imageUrl', label: '展示图（详情页）', type: 'image' },
      { name: 'tags', label: '标签', type: 'tags' },
      { name: 'highlight', label: '亮点', type: 'text' },
      { name: 'featured', label: '是否推荐', type: 'boolean', batch: true },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'barrages',
    label: '树洞弹幕',
    columns: ['id', 'nickname', 'mood', 'content', 'likeCount'],
    fields: [
      { name: 'userId', label: '用户 ID', type: 'number' },
      { name: 'nickname', label: '昵称', type: 'text' },
      { name: 'mood', label: '心情', type: 'text' },
      { name: 'content', label: '内容', type: 'textarea' },
      { name: 'likeCount', label: '点赞数', type: 'number' },
      { name: 'liked', label: '已点赞', type: 'boolean' },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'called-texts',
    label: '疗愈文本',
    columns: ['id', 'title', 'tag', 'readTime', 'sortIndex'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'title', label: '标题', type: 'text' },
      { name: 'tag', label: '标签', type: 'text' },
      { name: 'content', label: '内容', type: 'textarea' },
      { name: 'summary', label: '摘要', type: 'textarea' },
      { name: 'imageUrl', label: '配图', type: 'image', ratio: 16 / 10 },
      { name: 'readTime', label: '阅读时长', type: 'text' },
      { name: 'sortIndex', label: '排序值', type: 'number' },
      { name: 'audioUrl', label: '音频（可导入本地文件）', type: 'audio' },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'musics',
    label: '音乐曲库',
    columns: ['id', 'title', 'artist', 'album'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'title', label: '歌名', type: 'text' },
      { name: 'artist', label: '歌手', type: 'text' },
      { name: 'album', label: '专辑', type: 'text' },
      { name: 'coverUrl', label: '封面', type: 'image', ratio: 1 },
      { name: 'musicUrl', label: '音频（可导入本地文件）', type: 'audio' },
      { name: 'lyric', label: '歌词', type: 'textarea' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'comments',
    label: '评论管理',
    columns: ['id', 'articleId', 'nickname', 'content', 'likeCount'],
    fields: [
      { name: 'articleId', label: '文章 ID', type: 'number' },
      { name: 'nickname', label: '昵称', type: 'text' },
      { name: 'content', label: '内容', type: 'textarea' },
      { name: 'likeCount', label: '点赞数', type: 'number' },
      { name: 'createdAt', label: '创建时间', type: 'datetime' }
    ]
  },
  {
    key: 'friend-links',
    label: '友情链接',
    columns: ['id', 'siteName', 'siteUrl', 'sortIndex', 'visible'],
    fields: [
      { name: 'id', label: 'ID', type: 'number' },
      { name: 'siteName', label: '站点名称', type: 'text' },
      { name: 'siteUrl', label: '站点地址', type: 'text' },
      { name: 'logoUrl', label: 'Logo 地址', type: 'text' },
      { name: 'description', label: '站点描述', type: 'text' },
      { name: 'sortIndex', label: '排序', type: 'number' },
      { name: 'visible', label: '可见', type: 'boolean', default: true, batch: true },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  },
  {
    key: 'bangumi-records',
    label: '番剧记录',
    columns: ['id', 'coverUrl', 'nameCn', 'category', 'status', 'watchedEps', 'rating', 'visible'],
    fields: [
      { name: 'subjectId', label: 'bgm 条目 ID', type: 'number' },
      { name: 'name', label: '原名', type: 'text' },
      { name: 'nameCn', label: '中文名', type: 'text' },
      { name: 'coverUrl', label: '封面（竖版 2:3）', type: 'image', ratio: 2 / 3 },
      { name: 'status', label: '状态', type: 'select', options: ['想看', '在看', '看过', '搁置', '弃番'], batch: true },
      { name: 'rating', label: '个人评分（1-10，可空）', type: 'number', batch: true },
      { name: 'watchedEps', label: '已看集数', type: 'number' },
      { name: 'totalEps', label: '总集数', type: 'number' },
      { name: 'category', label: '分类（热血 / 日常 / 奇幻…）', type: 'text' },
      { name: 'tags', label: '标签', type: 'tags' },
      { name: 'summary', label: '简介', type: 'textarea' },
      { name: 'sortIndex', label: '排序（越小越靠前）', type: 'number' },
      { name: 'visible', label: '在页面显示', type: 'boolean', default: true, batch: true },
      { name: 'score', label: 'bgm 均分（可空）', type: 'number' },
      { name: 'airDate', label: '放送日期（如 2023-09-29）', type: 'text' },
      { name: 'platform', label: '放送形态（TV / 剧场版 / OVA）', type: 'text' },
      { name: 'rank', label: 'bgm 排名（可空）', type: 'number' },
      { name: 'ratingTotal', label: 'bgm 评分人数（可空）', type: 'number' },
      { name: 'createdAt', label: '创建时间', type: 'datetime' },
      { name: 'updatedAt', label: '更新时间', type: 'datetime' }
    ]
  }
]

export default resourceSchemas
