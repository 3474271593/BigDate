-- Schema only, extracted from supplied shop.sql. No original row data.
SET NAMES utf8;

CREATE TABLE `accreds` (
  `accredId` int(11) NOT NULL AUTO_INCREMENT,
  `accredName` varchar(50) NOT NULL,
  `accredImg` varchar(150) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`accredId`),
  KEY `dataFlag` (`dataFlag`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `ad_positions` (
  `positionId` int(11) NOT NULL AUTO_INCREMENT,
  `positionType` tinyint(4) NOT NULL DEFAULT '0',
  `positionName` varchar(100) NOT NULL,
  `positionWidth` int(11) NOT NULL DEFAULT '0',
  `positionHeight` int(11) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `positionCode` varchar(20) DEFAULT NULL,
  `apSort` int(11) NOT NULL,
  PRIMARY KEY (`positionId`),
  KEY `dataFlag` (`positionType`) USING BTREE,
  KEY `positionCode` (`positionCode`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `addons` (
  `addonId` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(40) NOT NULL COMMENT '插件名或标识',
  `title` varchar(20) NOT NULL DEFAULT '' COMMENT '中文名',
  `description` text COMMENT '插件描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态',
  `config` text COMMENT '配置',
  `author` varchar(40) DEFAULT '' COMMENT '作者',
  `version` varchar(20) DEFAULT '' COMMENT '版本号',
  `createTime` datetime NOT NULL COMMENT '安装时间',
  `dataFlag` tinyint(4) DEFAULT '1',
  `isConfig` tinyint(4) DEFAULT '0',
  `updateTime` datetime DEFAULT NULL,
  PRIMARY KEY (`addonId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ads` (
  `adId` int(11) NOT NULL AUTO_INCREMENT,
  `adPositionId` int(11) NOT NULL DEFAULT '0',
  `adFile` varchar(150) NOT NULL,
  `adName` varchar(100) NOT NULL,
  `adURL` varchar(255) DEFAULT NULL,
  `adStartDate` date NOT NULL,
  `adEndDate` date NOT NULL,
  `adSort` int(11) NOT NULL DEFAULT '0',
  `adClickNum` int(11) NOT NULL DEFAULT '0',
  `positionType` tinyint(4) DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  `subTitle` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`adId`),
  KEY `adPositionId` (`adPositionId`,`adStartDate`,`adEndDate`),
  KEY `adPositionId_2` (`adPositionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `areas` (
  `areaId` int(11) NOT NULL AUTO_INCREMENT COMMENT '区域ID',
  `parentId` int(11) NOT NULL COMMENT '父ID',
  `areaName` varchar(100) NOT NULL COMMENT '地区名称',
  `isShow` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否显示	0:是 1:否',
  `areaSort` int(11) NOT NULL DEFAULT '0',
  `areaKey` char(10) DEFAULT NULL COMMENT '地区首字母',
  `areaType` tinyint(4) NOT NULL DEFAULT '1' COMMENT '级别标志1：省，2：市，3：县区',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1' COMMENT '删除标志 -1:删除 1:有效',
  `createTime` varchar(25) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`areaId`),
  KEY `isShow` (`isShow`,`dataFlag`),
  KEY `areaType` (`areaType`),
  KEY `parentId` (`parentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `article_cats` (
  `catId` int(11) NOT NULL AUTO_INCREMENT,
  `parentId` int(11) NOT NULL DEFAULT '0',
  `catType` tinyint(4) NOT NULL DEFAULT '0',
  `isShow` tinyint(4) NOT NULL DEFAULT '1',
  `catName` varchar(20) NOT NULL,
  `catSort` int(11) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`catId`),
  KEY `isShow` (`catType`,`dataFlag`,`isShow`) USING BTREE,
  KEY `parentId` (`dataFlag`,`parentId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `articles` (
  `articleId` int(11) NOT NULL AUTO_INCREMENT,
  `catId` int(11) NOT NULL,
  `articleTitle` varchar(200) NOT NULL,
  `isShow` tinyint(4) NOT NULL DEFAULT '1',
  `articleContent` longtext,
  `articleKey` varchar(200) DEFAULT NULL,
  `staffId` int(11) NOT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  `solve` int(10) unsigned DEFAULT '0' COMMENT '解决数',
  `unsolve` int(10) unsigned DEFAULT '0' COMMENT '未解决数',
  `coverImg` varchar(150) DEFAULT NULL,
  `visitorNum` int(10) unsigned DEFAULT '0' COMMENT '文章浏览量',
  `TypeStatus` int(10) DEFAULT '1',
  `likeNum` int(50) DEFAULT '0',
  `catSort` int(11) DEFAULT '0',
  `articleDesc` varchar(500) DEFAULT NULL COMMENT '文章描述',
  PRIMARY KEY (`articleId`),
  KEY `catId` (`catId`,`isShow`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `attributes` (
  `attrId` int(11) NOT NULL AUTO_INCREMENT,
  `goodsCatId` int(11) NOT NULL DEFAULT '0',
  `goodsCatPath` varchar(100) NOT NULL,
  `attrName` varchar(100) NOT NULL,
  `attrType` tinyint(4) NOT NULL DEFAULT '0',
  `attrVal` text,
  `attrSort` int(11) NOT NULL DEFAULT '0',
  `isShow` tinyint(4) DEFAULT '1',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`attrId`),
  KEY `shopId` (`goodsCatId`,`isShow`,`dataFlag`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `banks` (
  `bankId` int(11) NOT NULL AUTO_INCREMENT,
  `bankName` varchar(50) NOT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`bankId`),
  KEY `bankFlag` (`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `brands` (
  `brandId` int(11) NOT NULL AUTO_INCREMENT,
  `brandName` varchar(100) NOT NULL,
  `brandImg` varchar(150) NOT NULL,
  `brandDesc` text,
  `createTime` datetime NOT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `sortNo` int(11) DEFAULT '0',
  PRIMARY KEY (`brandId`),
  KEY `brandFlag` (`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `carts` (
  `cartId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL DEFAULT '0',
  `isCheck` tinyint(4) NOT NULL DEFAULT '1',
  `goodsId` int(11) NOT NULL DEFAULT '0',
  `goodsSpecId` varchar(200) NOT NULL DEFAULT '0',
  `cartNum` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`cartId`),
  KEY `userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cash_configs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `targetType` tinyint(4) NOT NULL DEFAULT '0',
  `targetId` int(11) NOT NULL,
  `accType` tinyint(4) NOT NULL DEFAULT '0',
  `accTargetId` int(11) NOT NULL DEFAULT '0',
  `accAreaId` int(11) DEFAULT NULL,
  `accNo` varchar(100) NOT NULL,
  `accUser` varchar(100) NOT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `targetType` (`targetType`,`targetId`,`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cash_draws` (
  `cashId` int(11) NOT NULL AUTO_INCREMENT,
  `cashNo` varchar(50) NOT NULL,
  `targetType` tinyint(4) NOT NULL DEFAULT '0',
  `targetId` int(11) NOT NULL DEFAULT '0',
  `money` decimal(11,2) NOT NULL DEFAULT '0.00',
  `accType` tinyint(4) NOT NULL DEFAULT '0',
  `accTargetName` varchar(100) DEFAULT NULL,
  `accAreaName` varchar(100) DEFAULT NULL,
  `accNo` varchar(100) NOT NULL,
  `accUser` varchar(100) DEFAULT NULL,
  `cashSatus` tinyint(4) NOT NULL DEFAULT '0',
  `cashRemarks` varchar(255) DEFAULT NULL,
  `cashConfigId` int(11) NOT NULL,
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`cashId`),
  KEY `targetType` (`targetType`,`targetId`),
  KEY `cashNo` (`cashNo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cat_brands` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `catId` int(11) DEFAULT NULL,
  `brandId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `catId` (`catId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cat_shops` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL DEFAULT '0',
  `catId` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `catId` (`catId`),
  KEY `shopId` (`shopId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `charge_items` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `chargeMoney` int(11) DEFAULT '0',
  `giveMoney` decimal(11,1) DEFAULT '0.0',
  `itemSort` int(11) DEFAULT '0',
  `dataFlag` tinyint(4) DEFAULT '1',
  `createTime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `crons` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cronName` varchar(100) NOT NULL,
  `cronCode` varchar(20) NOT NULL,
  `isEnable` tinyint(4) NOT NULL DEFAULT '0',
  `isRunning` tinyint(4) NOT NULL DEFAULT '0',
  `cronJson` text,
  `cronUrl` varchar(255) NOT NULL,
  `cronDesc` varchar(255) DEFAULT NULL,
  `cronCycle` tinyint(4) NOT NULL DEFAULT '0',
  `cronDay` tinyint(4) DEFAULT '1',
  `cronWeek` tinyint(4) DEFAULT '0',
  `cronHour` tinyint(4) DEFAULT NULL,
  `cronMinute` varchar(255) DEFAULT NULL,
  `runTime` varchar(20) DEFAULT NULL,
  `nextTime` varchar(20) DEFAULT NULL,
  `isRunSuccess` tinyint(4) NOT NULL DEFAULT '1',
  `author` varchar(255) DEFAULT NULL,
  `authorUrl` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `data_cats` (
  `catId` int(11) NOT NULL AUTO_INCREMENT,
  `catName` varchar(255) NOT NULL,
  `dataFlag` tinyint(4) DEFAULT '1' COMMENT '数据有效标志1:有效 -1:无效',
  `catCode` varchar(255) NOT NULL COMMENT '数据分类代码',
  PRIMARY KEY (`catId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `datas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `catId` int(11) NOT NULL DEFAULT '0',
  `dataName` varchar(255) NOT NULL,
  `dataVal` varchar(255) NOT NULL,
  `dataSort` int(11) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) DEFAULT '1' COMMENT '数据有效标志1:有效 -1:无效',
  PRIMARY KEY (`id`),
  KEY `catId` (`catId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `express` (
  `expressId` int(11) NOT NULL AUTO_INCREMENT,
  `expressName` varchar(50) NOT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `expressCode` varchar(50) DEFAULT '',
  PRIMARY KEY (`expressId`),
  KEY `dataFlag` (`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `favorites` (
  `favoriteId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL DEFAULT '0',
  `favoriteType` tinyint(4) NOT NULL DEFAULT '0',
  `targetId` int(11) NOT NULL,
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`favoriteId`),
  UNIQUE KEY `favoriteId` (`userId`,`favoriteType`,`targetId`) USING BTREE,
  KEY `userId` (`userId`,`favoriteType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `friendlinks` (
  `friendlinkId` int(11) NOT NULL AUTO_INCREMENT,
  `friendlinkIco` varchar(150) DEFAULT '',
  `friendlinkName` varchar(50) NOT NULL DEFAULT '',
  `friendlinkUrl` varchar(150) NOT NULL DEFAULT '',
  `friendlinkSort` int(11) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`friendlinkId`),
  KEY `dataFlag` (`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `goods` (
  `goodsId` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '商品id',
  `goodsSn` varchar(20) NOT NULL COMMENT '商品编号',
  `productNo` varchar(20) NOT NULL COMMENT '商品货号',
  `goodsName` varchar(200) NOT NULL COMMENT '商品名称',
  `goodsImg` varchar(150) NOT NULL COMMENT '商品图片',
  `shopId` bigint(11) NOT NULL COMMENT '门店ID',
  `goodsType` tinyint(4) NOT NULL DEFAULT '0' COMMENT '货物类型',
  `marketPrice` decimal(11,2) NOT NULL DEFAULT '0.00' COMMENT '市场价',
  `shopPrice` decimal(11,2) NOT NULL DEFAULT '0.00' COMMENT '门店价',
  `warnStock` bigint(11) NOT NULL DEFAULT '0' COMMENT '预警库存',
  `goodsStock` bigint(11) NOT NULL DEFAULT '0' COMMENT '商品总库存',
  `goodsUnit` char(10) NOT NULL COMMENT '单位',
  `goodsTips` text COMMENT '促销信息',
  `isSale` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否上架	0:不上架 1:上架',
  `isBest` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否精品	0:否 1:是',
  `isHot` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否热销产品	0:否 1:是',
  `isNew` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否新品	0:否 1:是',
  `isRecom` tinyint(4) DEFAULT '0' COMMENT '是否推荐	0:否 1:是',
  `goodsCatIdPath` varchar(255) DEFAULT NULL COMMENT '商品分类ID路径	catId1_catId2_catId3',
  `goodsCatId` int(11) NOT NULL COMMENT 'goodsCatId	最后一级商品分类ID',
  `shopCatId1` int(11) NOT NULL COMMENT '门店商品分类第一级ID',
  `shopCatId2` int(11) NOT NULL COMMENT '门店商品第二级分类ID',
  `brandId` int(11) DEFAULT '0' COMMENT '品牌ID',
  `goodsDesc` text NOT NULL COMMENT '商品描述',
  `goodsStatus` tinyint(4) NOT NULL DEFAULT '0' COMMENT '商品状态	-1:违规 0:未审核 1:已审核',
  `saleNum` int(11) NOT NULL DEFAULT '0' COMMENT '总销售量',
  `saleTime` varchar(25) NOT NULL COMMENT '上架时间',
  `visitNum` int(11) DEFAULT '0' COMMENT '访问数',
  `appraiseNum` int(11) DEFAULT '0' COMMENT '评价书',
  `isSpec` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否有规格	0:没有 1:有',
  `gallery` text COMMENT '商品相册',
  `goodsSeoKeywords` varchar(200) DEFAULT NULL COMMENT '商品SEO关键字',
  `illegalRemarks` varchar(255) DEFAULT NULL COMMENT '状态说明	一般用于说明拒绝原因',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1' COMMENT '	删除标志	-1:删除 1:有效',
  `createTime` varchar(25) NOT NULL,
  `isFreeShipping` tinyint(4) DEFAULT '0',
  `goodsSerachKeywords` text,
  PRIMARY KEY (`goodsId`),
  KEY `shopId` (`shopId`) USING BTREE,
  KEY `goodsStatus` (`goodsStatus`,`dataFlag`,`isSale`) USING BTREE,
  KEY `goodsCatIdPath` (`goodsCatIdPath`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `goods_appraises` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `shopId` bigint(11) NOT NULL DEFAULT '0' COMMENT '门店id',
  `orderId` bigint(11) NOT NULL DEFAULT '0' COMMENT '订单ID',
  `goodsId` bigint(11) NOT NULL DEFAULT '0' COMMENT '评价对象-商品id',
  `goodsSpecId` int(11) NOT NULL DEFAULT '0' COMMENT '商品规格id',
  `userId` bigint(11) NOT NULL DEFAULT '0' COMMENT '用户id',
  `goodsScore` int(11) NOT NULL DEFAULT '0' COMMENT '商品评分',
  `serviceScore` int(11) NOT NULL DEFAULT '0' COMMENT '服务评分',
  `timeScore` int(11) NOT NULL DEFAULT '0' COMMENT '时效评分',
  `content` text NOT NULL COMMENT '评价内容',
  `shopReply` text COMMENT '店铺回复',
  `images` text COMMENT '上传图片',
  `isShow` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否显示 0：不显示，1：显示',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1' COMMENT '有效状态',
  `createTime` varchar(25) NOT NULL COMMENT '创建时间',
  `replyTime` varchar(25) DEFAULT NULL COMMENT '回复时间',
  PRIMARY KEY (`id`),
  KEY `shopId` (`shopId`),
  KEY `goodsId` (`goodsId`,`goodsSpecId`,`dataFlag`,`isShow`) USING BTREE,
  KEY `userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `goods_attributes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL,
  `goodsId` int(11) NOT NULL,
  `attrId` int(11) NOT NULL,
  `attrVal` text NOT NULL,
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `shopId` (`shopId`),
  KEY `goodsId` (`goodsId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `goods_cats` (
  `catId` int(11) NOT NULL AUTO_INCREMENT COMMENT '品类ID',
  `parentId` int(11) NOT NULL COMMENT '父ID',
  `catName` varchar(20) NOT NULL COMMENT '分类名称',
  `isShow` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否显示	0:隐藏 1:显示',
  `isFloor` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否显示楼层	0:不显示 1:显示',
  `catSort` int(11) NOT NULL DEFAULT '0' COMMENT '排序号',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1' COMMENT '删除标志	1:有效 -1：删除',
  `createTime` varchar(25) NOT NULL COMMENT '建立时间',
  `commissionRate` decimal(11,2) DEFAULT '-1.00' COMMENT '商品佣金比例	-1代表使用上级父类的佣金设置',
  `catImg` varchar(150) DEFAULT NULL,
  `subTitle` varchar(150) DEFAULT '' COMMENT '楼层副标题',
  `simpleName` varchar(20) NOT NULL COMMENT '简写名称',
  `seoTitle` varchar(200) NOT NULL COMMENT '分类SEO标题',
  `seoKeywords` varchar(200) NOT NULL COMMENT '分类SEO关键字',
  `seoDes` varchar(200) NOT NULL COMMENT '分类SEO描述',
  `catListTheme` varchar(200) NOT NULL DEFAULT 'goods_list' COMMENT '商品分类列表风格',
  `detailTheme` varchar(200) NOT NULL DEFAULT 'goods_detail' COMMENT '商品详情风格',
  `mobileCatListTheme` varchar(200) NOT NULL DEFAULT 'goods_list' COMMENT '移动端商品分类列表风格',
  `mobileDetailTheme` varchar(200) NOT NULL DEFAULT 'goods_detail' COMMENT '移动端商品详情风格',
  `wechatCatListTheme` varchar(200) NOT NULL DEFAULT 'goods_list' COMMENT '微信端商品分类列表风格',
  `wechatDetailTheme` varchar(200) NOT NULL DEFAULT 'goods_detail' COMMENT '微信端商品详情风格',
  `cat_level` tinyint(4) DEFAULT '0' COMMENT '分类级别，共3级',
  PRIMARY KEY (`catId`),
  KEY `parentId` (`parentId`,`isShow`,`dataFlag`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `goods_consult` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `goodsId` int(10) unsigned NOT NULL COMMENT '商品id',
  `userId` int(10) unsigned DEFAULT NULL COMMENT '用户id',
  `consultType` tinyint(3) unsigned DEFAULT NULL COMMENT '咨询类别',
  `consultContent` varchar(500) DEFAULT NULL COMMENT '咨询内容',
  `createTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '咨询时间',
  `reply` varchar(255) NOT NULL DEFAULT '' COMMENT '商家回复',
  `replyTime` datetime DEFAULT NULL COMMENT '回复时间',
  `dataFlag` tinyint(4) DEFAULT '1' COMMENT '数据有效标志',
  `isShow` tinyint(4) DEFAULT '1' COMMENT '是否显示数据',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `goods_scores` (
  `scoreId` int(11) NOT NULL AUTO_INCREMENT,
  `goodsId` int(11) NOT NULL DEFAULT '0',
  `shopId` int(11) NOT NULL DEFAULT '0',
  `totalScore` int(11) NOT NULL DEFAULT '0',
  `totalUsers` int(11) NOT NULL DEFAULT '0',
  `goodsScore` int(11) NOT NULL DEFAULT '0',
  `goodsUsers` int(11) NOT NULL DEFAULT '0',
  `serviceScore` int(11) NOT NULL DEFAULT '0',
  `serviceUsers` int(11) NOT NULL DEFAULT '0',
  `timeScore` int(11) NOT NULL DEFAULT '0',
  `timeUsers` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`scoreId`),
  KEY `goodsId` (`goodsId`,`shopId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `goods_specs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL DEFAULT '0',
  `goodsId` int(11) NOT NULL DEFAULT '0',
  `productNo` varchar(20) NOT NULL,
  `specIds` varchar(255) NOT NULL,
  `marketPrice` decimal(11,2) NOT NULL DEFAULT '0.00',
  `specPrice` decimal(11,2) NOT NULL DEFAULT '0.00',
  `specStock` int(11) NOT NULL DEFAULT '0',
  `warnStock` int(11) NOT NULL DEFAULT '0',
  `saleNum` int(11) NOT NULL DEFAULT '0',
  `isDefault` tinyint(4) DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `shopId` (`goodsId`,`dataFlag`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `goods_virtuals` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL,
  `goodsId` int(11) NOT NULL,
  `cardNo` varchar(20) NOT NULL,
  `cardPwd` varchar(20) NOT NULL,
  `orderId` int(11) NOT NULL DEFAULT '0',
  `orderNo` varchar(20) DEFAULT NULL,
  `isUse` tinyint(4) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `shopId` (`shopId`,`cardNo`),
  KEY `goodsId` (`goodsId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `home_menus` (
  `menuId` int(11) NOT NULL AUTO_INCREMENT,
  `parentId` int(11) NOT NULL DEFAULT '0',
  `menuName` varchar(100) NOT NULL,
  `menuUrl` varchar(100) NOT NULL,
  `menuOtherUrl` text,
  `menuType` tinyint(4) NOT NULL DEFAULT '0',
  `isShow` tinyint(4) DEFAULT '1',
  `menuSort` int(11) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime DEFAULT NULL,
  `menuMark` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`menuId`),
  KEY `parentId` (`parentId`,`isShow`,`dataFlag`),
  KEY `menuType` (`menuType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `hooks` (
  `hookId` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(40) NOT NULL DEFAULT '' COMMENT '钩子名称',
  `hookRemarks` text NOT NULL COMMENT '描述',
  `hookType` tinyint(1) NOT NULL DEFAULT '1' COMMENT '类型',
  `updateTime` datetime NOT NULL COMMENT '更新时间',
  `addons` text,
  PRIMARY KEY (`hookId`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `images` (
  `imgId` int(11) NOT NULL AUTO_INCREMENT,
  `fromType` tinyint(4) NOT NULL DEFAULT '0',
  `dataId` int(11) NOT NULL DEFAULT '0',
  `imgPath` varchar(150) NOT NULL,
  `imgSize` int(11) NOT NULL DEFAULT '0',
  `isUse` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  `fromTable` varchar(50) DEFAULT NULL,
  `ownId` int(11) DEFAULT NULL,
  `dataFlag` tinyint(4) DEFAULT '1',
  PRIMARY KEY (`imgId`),
  KEY `isUse` (`isUse`,`fromTable`,`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `informs` (
  `informId` int(11) NOT NULL AUTO_INCREMENT,
  `informTargetId` int(11) NOT NULL,
  `goodId` int(11) NOT NULL,
  `shopId` int(11) NOT NULL,
  `informType` int(11) NOT NULL DEFAULT '1',
  `informContent` text,
  `informAnnex` text NOT NULL,
  `informTime` datetime NOT NULL,
  `informStatus` tinyint(4) NOT NULL,
  `respondContent` text,
  `finalHandleStaffId` int(11) DEFAULT NULL,
  `finalHandleTime` datetime DEFAULT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`informId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `invoices` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `invoiceHead` varchar(255) NOT NULL COMMENT '发票抬头',
  `invoiceCode` varchar(255) NOT NULL DEFAULT '' COMMENT '纳税人识别号',
  `userId` int(10) unsigned NOT NULL COMMENT '用户id',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1' COMMENT '数据有效标记',
  `createTime` datetime NOT NULL COMMENT '数据创建时间',
  PRIMARY KEY (`id`),
  KEY `userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_moneys` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `targetType` tinyint(4) NOT NULL DEFAULT '0',
  `targetId` int(11) NOT NULL DEFAULT '0',
  `dataId` int(11) NOT NULL DEFAULT '0',
  `dataSrc` varchar(20) DEFAULT NULL,
  `remark` text NOT NULL,
  `moneyType` tinyint(4) NOT NULL DEFAULT '1',
  `money` decimal(11,2) NOT NULL DEFAULT '0.00',
  `tradeNo` varchar(100) DEFAULT NULL,
  `payType` varchar(20) DEFAULT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_operates` (
  `operateId` int(11) NOT NULL AUTO_INCREMENT,
  `staffId` int(11) NOT NULL DEFAULT '0',
  `operateTime` datetime NOT NULL,
  `menuId` int(11) NOT NULL,
  `operateDesc` varchar(255) NOT NULL,
  `operateUrl` varchar(255) NOT NULL,
  `content` longtext,
  `operateIP` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`operateId`),
  KEY `operateTime` (`staffId`,`menuId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_orders` (
  `logId` int(11) NOT NULL AUTO_INCREMENT,
  `orderId` int(11) NOT NULL DEFAULT '0',
  `orderStatus` int(11) NOT NULL,
  `logContent` varchar(255) NOT NULL,
  `logUserId` int(11) NOT NULL DEFAULT '0',
  `logType` tinyint(4) NOT NULL DEFAULT '0',
  `logTime` datetime NOT NULL,
  PRIMARY KEY (`logId`),
  KEY `orderId` (`orderId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_pay_params` (
  `logId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '0',
  `transId` varchar(50) DEFAULT NULL,
  `paramsVa` varchar(500) DEFAULT NULL,
  `payFrom` varchar(20) DEFAULT NULL,
  `createTime` datetime DEFAULT NULL,
  PRIMARY KEY (`logId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_pays` (
  `logId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL COMMENT '0',
  `transId` varchar(50) DEFAULT NULL,
  `createTime` datetime DEFAULT NULL,
  PRIMARY KEY (`logId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_sms` (
  `smsId` int(11) NOT NULL AUTO_INCREMENT,
  `smsSrc` tinyint(4) NOT NULL DEFAULT '0',
  `smsUserId` int(11) NOT NULL DEFAULT '0',
  `smsContent` varchar(255) NOT NULL,
  `smsPhoneNumber` varchar(11) NOT NULL,
  `smsReturnCode` varchar(255) NOT NULL,
  `smsCode` varchar(255) DEFAULT NULL,
  `smsFunc` varchar(50) NOT NULL,
  `smsIP` varchar(16) NOT NULL,
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`smsId`),
  KEY `smsPhoneNumber` (`smsPhoneNumber`),
  KEY `smsIP` (`smsIP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_staff_logins` (
  `loginId` int(11) NOT NULL AUTO_INCREMENT,
  `staffId` int(11) NOT NULL DEFAULT '0',
  `loginTime` datetime NOT NULL,
  `loginIp` varchar(16) NOT NULL,
  PRIMARY KEY (`loginId`),
  KEY `loginTime` (`loginTime`),
  KEY `staffId` (`staffId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_user_logins` (
  `loginId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `loginTime` datetime NOT NULL,
  `loginIp` varchar(16) NOT NULL,
  `loginSrc` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '0:商城  1:webapp  2:App',
  `loginRemark` varchar(30) DEFAULT NULL COMMENT '登录备注信息',
  PRIMARY KEY (`loginId`),
  KEY `loginTime` (`loginTime`),
  KEY `userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `logs` (
  `id` varchar(100) NOT NULL COMMENT '唯一标识',
  `url` varchar(200) DEFAULT NULL COMMENT '当前链接地址',
  `referer` varchar(200) DEFAULT NULL COMMENT '前一页链接地址',
  `keyword` varchar(50) DEFAULT NULL COMMENT '搜索引擎关键字',
  `type` varchar(10) DEFAULT NULL COMMENT '访问类型',
  `guid` varchar(50) DEFAULT NULL COMMENT '唯一用户ID',
  `page_id` varchar(200) DEFAULT NULL COMMENT 'tracker自动化打点的pageType + "." + pageId +".0.0.0."+uniId(已废弃)',
  `module_id` varchar(200) DEFAULT NULL COMMENT 'tracker自动化打点的tracker position（tp）(可废弃)',
  `link_id` varchar(200) DEFAULT NULL COMMENT 'tracker自动化打点的tracker content（tc）(可废弃)',
  `attached_info` varchar(50) DEFAULT NULL COMMENT '附加信息，记录了库存信息等',
  `session_id` varchar(50) DEFAULT NULL COMMENT '对应cookie.tracker_msessionid',
  `tracker_u` varchar(50) DEFAULT NULL COMMENT '网盟用户id',
  `tracker_type` varchar(200) DEFAULT NULL COMMENT '跟踪类型',
  `ip` varchar(200) DEFAULT NULL COMMENT '用户ip',
  `tracker_src` varchar(200) DEFAULT NULL COMMENT '页面来源',
  `cookie` varchar(200) DEFAULT NULL COMMENT '传入的cookie参数',
  `order_code` varchar(50) DEFAULT NULL COMMENT '订单编号',
  `track_time` varchar(50) DEFAULT NULL COMMENT '跟踪时间',
  `end_user_id` varchar(50) DEFAULT NULL COMMENT '客户ID',
  `first_link` varchar(200) DEFAULT NULL COMMENT '首次访问的链接',
  `session_view_no` varchar(50) DEFAULT NULL COMMENT '客户访问网站的顺序',
  `product_id` varchar(50) DEFAULT NULL COMMENT '产品ID',
  `cur_merchant_id` varchar(50) DEFAULT NULL COMMENT '用户当前选择的merchantId（已废弃）',
  `province_id` varchar(10) DEFAULT NULL COMMENT '省份ID',
  `city_id` varchar(10) DEFAULT NULL COMMENT '城市ID',
  `fee` varchar(10) DEFAULT NULL COMMENT '判断搜索引擎关键字是否收费',
  `edm_activity` varchar(10) DEFAULT NULL COMMENT 'EDM邮件是否被打开,Y为打开',
  `edm_email` varchar(50) DEFAULT NULL COMMENT 'EDM邮件地址或者邮件ID',
  `edm_job_id` varchar(50) DEFAULT NULL COMMENT '发送该邮件地址的edm jod id',
  `ie_version` varchar(255) DEFAULT NULL COMMENT '浏览器版本',
  `platform` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `internal_keyword` varchar(50) DEFAULT NULL COMMENT '站内搜索关键字',
  `result_sum` varchar(50) DEFAULT NULL COMMENT '搜索结果数',
  `current_page` varchar(50) DEFAULT NULL COMMENT '搜索当前页',
  `link_position` varchar(50) DEFAULT NULL COMMENT '链接位置',
  `button_position` varchar(50) DEFAULT NULL COMMENT '按钮位置',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `menus` (
  `menuId` int(11) NOT NULL AUTO_INCREMENT,
  `parentId` int(11) NOT NULL,
  `menuName` varchar(100) NOT NULL,
  `menuSort` int(11) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '0',
  `menuMark` varchar(50) DEFAULT NULL,
  `isShow` tinyint(4) DEFAULT '1',
  `menuIcon` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`menuId`),
  KEY `parentId` (`parentId`,`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `message_queues` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `msgType` tinyint(4) DEFAULT '0',
  `paramJson` text,
  `msgJson` text,
  `createTime` datetime DEFAULT NULL,
  `sendTime` datetime DEFAULT NULL,
  `sendStatus` tinyint(4) DEFAULT '0',
  `msgCode` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `msgType` tinyint(4) NOT NULL DEFAULT '0',
  `sendUserId` int(11) NOT NULL DEFAULT '0',
  `receiveUserId` int(11) NOT NULL DEFAULT '0',
  `msgContent` text NOT NULL,
  `msgStatus` tinyint(4) NOT NULL DEFAULT '0',
  `msgJson` varchar(255) DEFAULT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `receiveUserId` (`receiveUserId`,`dataFlag`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `mobile_btns` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `btnName` varchar(255) DEFAULT NULL,
  `btnSrc` tinyint(4) NOT NULL DEFAULT '0',
  `btnUrl` varchar(255) DEFAULT NULL,
  `btnImg` varchar(255) DEFAULT NULL,
  `addonsName` varchar(255) DEFAULT NULL,
  `btnSort` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `btnSrc` (`btnSrc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `navs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `navType` tinyint(4) NOT NULL DEFAULT '0',
  `navTitle` varchar(50) NOT NULL,
  `navUrl` varchar(100) NOT NULL,
  `isShow` tinyint(4) NOT NULL DEFAULT '1',
  `isOpen` tinyint(4) NOT NULL DEFAULT '0',
  `navSort` int(11) NOT NULL DEFAULT '0',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `navType` (`navType`,`isShow`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `order_complains` (
  `complainId` int(11) NOT NULL AUTO_INCREMENT,
  `orderId` int(11) NOT NULL DEFAULT '0',
  `complainType` tinyint(4) NOT NULL DEFAULT '1',
  `complainTargetId` int(11) NOT NULL DEFAULT '0',
  `respondTargetId` int(11) NOT NULL DEFAULT '0',
  `needRespond` tinyint(4) NOT NULL DEFAULT '0',
  `deliverRespondTime` datetime DEFAULT NULL,
  `complainContent` text NOT NULL,
  `complainAnnex` varchar(255) DEFAULT NULL,
  `complainStatus` tinyint(4) NOT NULL DEFAULT '0',
  `complainTime` datetime NOT NULL,
  `respondContent` text,
  `respondAnnex` varchar(255) DEFAULT NULL,
  `respondTime` datetime DEFAULT NULL,
  `finalResult` text,
  `finalResultTime` datetime DEFAULT NULL,
  `finalHandleStaffId` int(11) DEFAULT '0',
  PRIMARY KEY (`complainId`),
  KEY `complainStatus` (`complainStatus`),
  KEY `complainType` (`complainTargetId`,`complainType`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `order_express` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `orderId` int(11) NOT NULL,
  `orderGoodsId` varchar(100) NOT NULL,
  `deliveryTime` datetime DEFAULT NULL,
  `isExpress` tinyint(4) DEFAULT '0' COMMENT '1:��Ҫ����  0:��������',
  `expressId` int(11) DEFAULT NULL,
  `expressNo` varchar(20) DEFAULT NULL,
  `createTime` datetime NOT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `orderId` (`orderId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `order_goods` (
  `ogId` bigint(11) NOT NULL AUTO_INCREMENT,
  `orderId` bigint(11) NOT NULL COMMENT '订单id',
  `goodsId` bigint(11) NOT NULL COMMENT '商品id',
  `goodsNum` bigint(11) NOT NULL DEFAULT '0' COMMENT '商品数量',
  `goodsPrice` decimal(11,2) NOT NULL DEFAULT '0.00' COMMENT '商品价格',
  `goodsSpecId` int(11) DEFAULT '0' COMMENT '商品规格id',
  `goodsSpecNames` varchar(500) DEFAULT NULL COMMENT '商品规格列表',
  `goodsName` varchar(200) NOT NULL COMMENT '商品名称',
  `goodsImg` varchar(150) NOT NULL COMMENT '商品图片',
  `extraJson` text COMMENT '额外信息',
  `goodsType` tinyint(4) NOT NULL DEFAULT '0',
  `commissionRate` decimal(11,2) DEFAULT '0.00' COMMENT '商品佣金比率',
  `goodsCode` varchar(20) DEFAULT NULL,
  `promotionJson` text,
  PRIMARY KEY (`ogId`),
  KEY `goodsId` (`goodsId`),
  KEY `orderId` (`orderId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `order_refunds` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `orderId` int(11) NOT NULL COMMENT '订单id',
  `goodsId` int(11) NOT NULL COMMENT '商品id',
  `refundTo` int(11) NOT NULL DEFAULT '0' COMMENT '接收退款用户',
  `refundReson` int(11) NOT NULL DEFAULT '0' COMMENT '用户申请退款原因ID',
  `refundOtherReson` varchar(255) DEFAULT NULL COMMENT '用户申请退款原因',
  `backMoney` decimal(11,2) NOT NULL DEFAULT '0.00' COMMENT '退款金额',
  `refundTradeNo` varchar(100) DEFAULT NULL COMMENT '退款流水号',
  `refundRemark` varchar(500) DEFAULT NULL COMMENT '退款备注',
  `refundTime` varchar(25) DEFAULT NULL COMMENT '退款时间',
  `shopRejectReason` varchar(255) DEFAULT NULL COMMENT '店铺不同意退款原因',
  `refundStatus` tinyint(4) NOT NULL DEFAULT '0' COMMENT '退款状态',
  `createTime` varchar(25) NOT NULL COMMENT '用户申请退款时间',
  PRIMARY KEY (`id`),
  KEY `orderId` (`orderId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `orderids` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `rnd` float(16,2) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `orders` (
  `orderId` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '订单id',
  `orderNo` varchar(20) NOT NULL COMMENT '订单编号',
  `shopId` bigint(11) NOT NULL COMMENT '门店id',
  `userId` bigint(11) NOT NULL COMMENT '用户id',
  `orderStatus` tinyint(4) NOT NULL DEFAULT '-2' COMMENT '订单状态 -3:用户拒收 -2:未付款的订单 -1：用户取消 0:待发货 1:配送中 2:用户确认收货',
  `goodsMoney` decimal(11,2) NOT NULL COMMENT '商品金额',
  `deliverType` tinyint(4) NOT NULL DEFAULT '0' COMMENT '收货方式 	0:送货上门 1:自提',
  `deliverMoney` decimal(11,2) NOT NULL DEFAULT '0.00' COMMENT '运费',
  `totalMoney` decimal(11,2) NOT NULL COMMENT '订单金额（包括运费）',
  `realTotalMoney` decimal(11,2) NOT NULL DEFAULT '0.00' COMMENT '实际订单金额（折扣后金额）',
  `payType` tinyint(4) NOT NULL DEFAULT '0' COMMENT '支付方式,0:未知;1:支付宝，2：微信;3、现金；4、其他',
  `isPay` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否支付	0:未支付 1:已支付',
  `areaId` int(11) NOT NULL COMMENT '区域最低一级',
  `areaIdPath` varchar(255) DEFAULT NULL COMMENT '区域idpath',
  `userName` varchar(20) NOT NULL COMMENT '收件人姓名',
  `userAddress` varchar(255) NOT NULL COMMENT '收件人地址',
  `userPhone` char(20) DEFAULT NULL COMMENT '收件人电话',
  `orderScore` int(11) NOT NULL DEFAULT '0' COMMENT '订单所得积分',
  `isInvoice` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否开发票 1:需要 0:不需要',
  `invoiceClient` varchar(255) DEFAULT NULL COMMENT '发票抬头',
  `orderRemarks` varchar(255) DEFAULT NULL COMMENT '订单备注',
  `orderSrc` tinyint(4) NOT NULL DEFAULT '0' COMMENT '订单来源	0:商城 1:微信 2:手机版 3:安卓App 4:苹果App',
  `needPay` decimal(11,2) DEFAULT '0.00' COMMENT '需缴费用',
  `payRand` int(11) DEFAULT '1' COMMENT '货币单位',
  `orderType` int(11) DEFAULT '0' COMMENT '订单类型',
  `isRefund` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否退款	0:否 1：是',
  `isAppraise` tinyint(4) DEFAULT '0' COMMENT '是否点评	0:未点评 1:已点评',
  `cancelReason` int(11) DEFAULT '0' COMMENT '取消原因ID',
  `rejectReason` int(11) DEFAULT '0' COMMENT '用户拒绝原因ID',
  `rejectOtherReason` varchar(255) DEFAULT NULL COMMENT '拒收原因',
  `isClosed` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否订单已完结	0：未完结 1:已完结',
  `goodsSearchKeys` text,
  `orderunique` varchar(50) NOT NULL COMMENT '订单流水号',
  `receiveTime` varchar(25) DEFAULT NULL COMMENT '收货时间',
  `deliveryTime` varchar(25) DEFAULT NULL COMMENT '发货时间',
  `tradeNo` varchar(100) DEFAULT NULL COMMENT '在线支付交易流水',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1' COMMENT '订单有效标志	-1：删除 1:有效',
  `createTime` varchar(25) NOT NULL COMMENT '下单时间',
  `settlementId` int(11) DEFAULT '0' COMMENT '是否结算，大于0的话则是结算ID',
  `commissionFee` decimal(11,2) DEFAULT '0.00' COMMENT '	订单应收佣金',
  `scoreMoney` decimal(11,2) DEFAULT '0.00' COMMENT '积分抵扣金额',
  `useScore` int(11) DEFAULT '0' COMMENT '花费积分',
  `orderCode` varchar(20) DEFAULT 'order',
  `extraJson` text COMMENT '额外信息',
  `orderCodeTargetId` int(11) DEFAULT '0',
  `noticeDeliver` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '提醒发货 0:未提醒 1:已提醒',
  `invoiceJson` text COMMENT '发票信息',
  `lockCashMoney` decimal(11,2) DEFAULT '0.00' COMMENT '锁定提现金额',
  `payTime` varchar(25) DEFAULT NULL COMMENT '支付时间',
  `isBatch` tinyint(4) DEFAULT '0' COMMENT '是否拼单',
  `totalPayFee` int(11) DEFAULT '0' COMMENT '总支付金额',
  PRIMARY KEY (`orderId`),
  KEY `shopId` (`shopId`,`dataFlag`),
  KEY `userId` (`userId`,`dataFlag`),
  KEY `isRefund` (`isRefund`),
  KEY `orderStatus` (`orderStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `org` (
  `orgId` int(11) NOT NULL AUTO_INCREMENT COMMENT '组织ID',
  `parentId` int(11) NOT NULL COMMENT '父ID',
  `orgName` varchar(100) NOT NULL COMMENT '组织名称',
  `orgLevel` tinyint(4) NOT NULL DEFAULT '1' COMMENT '组织级别1;总部及大区级部门;2：总部下属的各个部门及基部门;3:具体工作部门',
  `managerCode` varchar(15) DEFAULT NULL COMMENT '主管工号',
  `isdelete` tinyint(4) NOT NULL DEFAULT '0' COMMENT '删除标志,1:删除;0:有效',
  `createTime` varchar(25) DEFAULT NULL COMMENT '创建时间',
  `updateTime` varchar(25) DEFAULT NULL COMMENT '最后修改时间',
  `isShow` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否显示,0:是 1:否',
  `orgType` tinyint(4) NOT NULL DEFAULT '1' COMMENT '组织类型,0:总裁办;1:研发;2:销售;3:运营;4:产品',
  PRIMARY KEY (`orgId`),
  KEY `parentId` (`parentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `payments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `payCode` varchar(20) DEFAULT NULL,
  `payName` varchar(255) DEFAULT NULL,
  `payDesc` text,
  `payOrder` int(11) DEFAULT '0',
  `payConfig` text,
  `enabled` tinyint(4) DEFAULT '0',
  `isOnline` tinyint(4) DEFAULT '0',
  `payFor` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `payCode` (`payCode`,`enabled`,`isOnline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `privileges` (
  `privilegeId` int(11) NOT NULL AUTO_INCREMENT,
  `menuId` int(11) NOT NULL,
  `privilegeCode` varchar(20) NOT NULL,
  `privilegeName` varchar(30) NOT NULL,
  `isMenuPrivilege` tinyint(4) NOT NULL DEFAULT '0',
  `privilegeUrl` varchar(255) DEFAULT NULL,
  `otherPrivilegeUrl` text,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `isEnable` tinyint(4) DEFAULT '1',
  PRIMARY KEY (`privilegeId`),
  UNIQUE KEY `privilegeCode` (`privilegeCode`),
  KEY `menuId` (`menuId`,`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `recommends` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `goodsCatId` int(11) NOT NULL DEFAULT '0',
  `dataType` tinyint(4) NOT NULL DEFAULT '0',
  `dataSrc` tinyint(4) DEFAULT '0',
  `dataId` int(11) NOT NULL DEFAULT '0',
  `dataSort` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `goodsCatId` (`goodsCatId`,`dataType`,`dataSrc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `roles` (
  `roleId` int(11) NOT NULL AUTO_INCREMENT,
  `roleName` varchar(30) NOT NULL,
  `roleDesc` varchar(255) DEFAULT NULL,
  `privileges` text,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`roleId`),
  KEY `roleFlag` (`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `settlements` (
  `settlementId` int(11) NOT NULL AUTO_INCREMENT,
  `settlementNo` varchar(20) NOT NULL,
  `settlementType` tinyint(4) NOT NULL DEFAULT '0',
  `shopId` int(11) NOT NULL,
  `settlementMoney` decimal(11,2) NOT NULL DEFAULT '0.00',
  `commissionFee` decimal(11,2) NOT NULL DEFAULT '0.00',
  `backMoney` decimal(11,2) NOT NULL DEFAULT '0.00',
  `settlementStatus` tinyint(4) NOT NULL DEFAULT '0',
  `settlementTime` datetime DEFAULT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`settlementId`),
  KEY `shopId` (`shopId`),
  KEY `settlementStatus` (`settlementStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_accreds` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `accredId` int(11) NOT NULL DEFAULT '0',
  `shopId` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `shopId` (`shopId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_cats` (
  `catId` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL,
  `parentId` int(11) NOT NULL,
  `isShow` tinyint(4) NOT NULL DEFAULT '1',
  `catName` varchar(100) NOT NULL,
  `catSort` int(11) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`catId`),
  KEY `parentId` (`isShow`,`dataFlag`) USING BTREE,
  KEY `shopId` (`shopId`,`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_configs` (
  `configId` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL,
  `shopTitle` varchar(255) DEFAULT NULL,
  `shopKeywords` varchar(255) DEFAULT NULL,
  `shopDesc` varchar(255) DEFAULT NULL,
  `shopBanner` varchar(150) DEFAULT NULL,
  `shopAds` text,
  `shopAdsUrl` text,
  `shopServicer` varchar(100) DEFAULT NULL,
  `shopHotWords` varchar(255) DEFAULT NULL,
  `shopStreetImg` varchar(150) DEFAULT '' COMMENT '店铺街背景',
  `shopMoveBanner` varchar(150) DEFAULT NULL,
  `shopHomeTheme` varchar(200) NOT NULL DEFAULT 'shop_home' COMMENT '店铺风格',
  `mobileShopHomeTheme` varchar(200) NOT NULL DEFAULT 'shop_home' COMMENT '手机端店铺风格',
  `wechatShopHomeTheme` varchar(200) NOT NULL DEFAULT 'shop_home' COMMENT '微信端店铺风格',
  PRIMARY KEY (`configId`),
  KEY `shopId` (`shopId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_extras` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL,
  `businessLicenceType` tinyint(4) DEFAULT '0',
  `businessLicence` varchar(100) DEFAULT NULL,
  `licenseAddress` varchar(500) DEFAULT NULL,
  `businessAreaPath` varchar(100) DEFAULT NULL,
  `legalPersonName` varchar(100) DEFAULT NULL,
  `establishmentDate` date DEFAULT NULL,
  `businessStartDate` date DEFAULT NULL,
  `businessEndDate` date DEFAULT NULL,
  `isLongbusinessDate` tinyint(4) DEFAULT '0',
  `registeredCapital` decimal(11,0) DEFAULT '0',
  `empiricalRange` varchar(1000) DEFAULT NULL,
  `legalCertificateType` tinyint(4) DEFAULT '0',
  `legalCertificate` varchar(50) DEFAULT NULL,
  `legalCertificateStartDate` date DEFAULT NULL,
  `legalCertificateEndDate` date DEFAULT NULL,
  `isLonglegalCertificateDate` tinyint(4) DEFAULT '0',
  `legalCertificateImg` varchar(150) DEFAULT NULL,
  `businessLicenceImg` varchar(150) DEFAULT NULL,
  `bankAccountPermitImg` varchar(150) DEFAULT NULL,
  `organizationCode` varchar(100) DEFAULT NULL,
  `organizationCodeStartDate` date DEFAULT NULL,
  `organizationCodeEndDate` date DEFAULT NULL,
  `isLongOrganizationCodeDate` tinyint(4) DEFAULT '0',
  `organizationCodeImg` varchar(150) DEFAULT NULL,
  `taxRegistrationCertificateImg` varchar(450) DEFAULT NULL,
  `taxpayerQualificationImg` varchar(150) DEFAULT NULL,
  `taxpayerType` tinyint(4) DEFAULT '0',
  `taxpayerNo` varchar(100) DEFAULT NULL,
  `applyLinkMan` varchar(50) DEFAULT NULL,
  `applyLinkTel` varchar(50) DEFAULT NULL,
  `applyLinkEmail` varchar(50) DEFAULT NULL,
  `isInvestment` tinyint(4) NOT NULL DEFAULT '0',
  `investmentStaff` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_freights` (
  `freightId` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL,
  `areaId2` int(11) NOT NULL,
  `freight` int(11) NOT NULL DEFAULT '0',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`freightId`),
  KEY `shopId` (`shopId`,`areaId2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_message_cats` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `msgDataId` int(11) DEFAULT '0',
  `msgType` tinyint(4) DEFAULT '0',
  `msgCode` varchar(100) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_roles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) DEFAULT '0',
  `roleName` varchar(100) DEFAULT NULL,
  `privilegeMsgs` text,
  `privilegeUrls` text,
  `createTime` datetime DEFAULT NULL,
  `dataFlag` tinyint(4) DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_scores` (
  `scoreId` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL,
  `totalScore` int(11) NOT NULL DEFAULT '0',
  `totalUsers` int(11) NOT NULL DEFAULT '0',
  `goodsScore` int(11) NOT NULL DEFAULT '0',
  `goodsUsers` int(11) NOT NULL DEFAULT '0',
  `serviceScore` int(11) NOT NULL DEFAULT '0',
  `serviceUsers` int(11) NOT NULL DEFAULT '0',
  `timeScore` int(11) NOT NULL DEFAULT '0',
  `timeUsers` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`scoreId`),
  UNIQUE KEY `shopId` (`shopId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_styles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `styleSys` varchar(255) DEFAULT NULL,
  `styleName` varchar(255) DEFAULT NULL,
  `styleCat` tinyint(4) unsigned DEFAULT '0',
  `stylePath` varchar(255) DEFAULT NULL,
  `screenshot` varchar(255) DEFAULT NULL,
  `isShow` tinyint(4) DEFAULT '1' COMMENT '1:显示  0:隐藏',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shop_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL DEFAULT '0',
  `userId` int(11) NOT NULL DEFAULT '0',
  `roleId` int(11) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shops` (
  `shopId` int(11) NOT NULL AUTO_INCREMENT COMMENT '商铺ID，自增',
  `shopSn` varchar(20) DEFAULT '',
  `userId` int(11) NOT NULL COMMENT '商铺联系人ID',
  `areaIdPath` varchar(255) DEFAULT '',
  `areaId` int(11) DEFAULT '0',
  `isSelf` tinyint(4) NOT NULL DEFAULT '0',
  `shopName` varchar(100) DEFAULT '' COMMENT '商铺名称',
  `shopkeeper` varchar(50) DEFAULT '',
  `telephone` varchar(20) DEFAULT '' COMMENT '联系人电话',
  `shopCompany` varchar(255) DEFAULT '' COMMENT '商家实体名称',
  `shopImg` varchar(150) DEFAULT '' COMMENT 'logo图片',
  `shopTel` varchar(40) DEFAULT '' COMMENT '商家联系电话',
  `shopQQ` varchar(50) DEFAULT NULL COMMENT '联系人QQ',
  `shopWangWang` varchar(50) DEFAULT NULL,
  `shopAddress` varchar(255) DEFAULT '' COMMENT '商家地址',
  `bankId` int(11) DEFAULT '0',
  `bankNo` varchar(20) DEFAULT '',
  `bankUserName` varchar(50) DEFAULT '',
  `isInvoice` tinyint(4) NOT NULL DEFAULT '0',
  `invoiceRemarks` varchar(255) DEFAULT NULL,
  `serviceStartTime` bigint(20) NOT NULL DEFAULT '1570419271000' COMMENT '服务开始时间',
  `serviceEndTime` bigint(20) NOT NULL DEFAULT '1570419271000' COMMENT '服务结束时间',
  `freight` int(11) DEFAULT '0',
  `shopAtive` tinyint(4) NOT NULL DEFAULT '1',
  `shopStatus` tinyint(4) NOT NULL DEFAULT '1' COMMENT '商铺状态',
  `statusDesc` varchar(255) DEFAULT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` date DEFAULT NULL,
  `shopMoney` decimal(11,2) DEFAULT '0.00',
  `lockMoney` decimal(11,2) DEFAULT '0.00',
  `noSettledOrderNum` int(11) DEFAULT '0',
  `noSettledOrderFee` decimal(11,2) DEFAULT '0.00',
  `paymentMoney` decimal(11,2) DEFAULT '0.00',
  `bankAreaId` int(11) DEFAULT '0',
  `bankAreaIdPath` varchar(100) DEFAULT NULL,
  `applyStatus` tinyint(4) DEFAULT '0',
  `applyDesc` varchar(255) DEFAULT NULL,
  `applyTime` datetime DEFAULT NULL,
  `applyStep` tinyint(4) DEFAULT '1',
  `shopNotice` varchar(300) DEFAULT NULL COMMENT '店铺公告',
  `rechargeMoney` decimal(11,2) DEFAULT '0.00' COMMENT '充值金额',
  `longitude` decimal(10,7) DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `mapLevel` int(11) DEFAULT '16',
  `BDcode` varchar(16) DEFAULT NULL COMMENT '公司管理人员code',
  PRIMARY KEY (`shopId`),
  KEY `shopStatus` (`shopStatus`,`dataFlag`) USING BTREE,
  KEY `areaIdPath` (`areaIdPath`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `spec_cats` (
  `catId` int(11) NOT NULL AUTO_INCREMENT,
  `goodsCatId` int(11) NOT NULL DEFAULT '0',
  `goodsCatPath` varchar(100) NOT NULL,
  `catName` varchar(255) NOT NULL,
  `isAllowImg` tinyint(4) NOT NULL DEFAULT '0',
  `isShow` tinyint(4) NOT NULL DEFAULT '1',
  `catSort` int(11) DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`catId`),
  KEY `shopId` (`goodsCatPath`,`dataFlag`),
  KEY `isShow` (`isShow`,`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `spec_items` (
  `itemId` int(11) NOT NULL AUTO_INCREMENT,
  `shopId` int(11) NOT NULL DEFAULT '0',
  `catId` int(11) NOT NULL DEFAULT '0',
  `goodsId` int(11) NOT NULL DEFAULT '0',
  `itemName` varchar(100) NOT NULL,
  `itemDesc` varchar(255) DEFAULT NULL,
  `itemImg` varchar(150) NOT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`itemId`),
  KEY `goodsId` (`goodsId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `staffs` (
  `staffId` int(11) NOT NULL AUTO_INCREMENT,
  `loginName` varchar(40) NOT NULL,
  `loginPwd` varchar(50) NOT NULL,
  `secretKey` int(32) NOT NULL,
  `staffName` varchar(50) NOT NULL,
  `staffNo` varchar(20) DEFAULT NULL,
  `staffPhoto` varchar(150) DEFAULT NULL,
  `staffRoleId` int(11) NOT NULL,
  `workStatus` tinyint(4) NOT NULL DEFAULT '1',
  `staffStatus` tinyint(4) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  `lastTime` datetime DEFAULT NULL,
  `lastIP` char(16) DEFAULT NULL,
  `wxOpenId` varchar(100) DEFAULT NULL,
  `staffPhone` char(11) DEFAULT NULL,
  PRIMARY KEY (`staffId`),
  KEY `loginName` (`loginName`),
  KEY `staffStatus` (`staffStatus`,`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `styles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `styleSys` varchar(20) DEFAULT NULL,
  `styleName` varchar(255) NOT NULL,
  `styleAuthor` varchar(255) DEFAULT NULL,
  `styleShopSite` varchar(11) DEFAULT NULL,
  `styleShopId` int(11) DEFAULT '0',
  `stylePath` varchar(255) NOT NULL,
  `isUse` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `isUse` (`isUse`,`styleSys`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `switchs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `homeURL` varchar(255) DEFAULT NULL,
  `mobileURL` varchar(255) DEFAULT NULL,
  `wechatURL` varchar(255) DEFAULT NULL,
  `urlMark` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `sys_configs` (
  `configId` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `fieldName` varchar(50) DEFAULT NULL COMMENT '字段名称',
  `fieldCode` varchar(50) DEFAULT NULL,
  `fieldValue` text,
  `fieldType` tinyint(4) DEFAULT '0',
  PRIMARY KEY (`configId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `template_msgs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tplType` tinyint(4) NOT NULL DEFAULT '0',
  `tplCode` varchar(50) NOT NULL,
  `tplExternaId` varchar(255) DEFAULT NULL,
  `tplContent` varchar(255) NOT NULL,
  `isEnbale` tinyint(4) DEFAULT '1',
  `tplDesc` text,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `status` tinyint(4) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `tplType` (`tplType`),
  KEY `tplCode` (`tplCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `third_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `thirdCode` varchar(20) DEFAULT NULL,
  `thirdOpenId` varchar(100) DEFAULT NULL,
  `createTime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `user_address` (
  `addressId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL,
  `userName` varchar(50) NOT NULL,
  `userPhone` varchar(20) DEFAULT NULL,
  `areaIdPath` varchar(255) NOT NULL DEFAULT '0',
  `areaId` int(11) NOT NULL DEFAULT '0',
  `userAddress` varchar(255) NOT NULL,
  `isDefault` tinyint(4) NOT NULL DEFAULT '0',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`addressId`),
  KEY `userId` (`userId`,`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `user_ranks` (
  `rankId` int(11) NOT NULL AUTO_INCREMENT,
  `rankName` varchar(20) NOT NULL,
  `startScore` int(11) NOT NULL DEFAULT '0',
  `endScore` int(11) NOT NULL DEFAULT '0',
  `userrankImg` varchar(150) DEFAULT NULL,
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`rankId`),
  KEY `startScore` (`startScore`,`endScore`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `user_scores` (
  `scoreId` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) NOT NULL DEFAULT '0',
  `score` int(11) NOT NULL DEFAULT '0',
  `dataSrc` int(11) NOT NULL DEFAULT '0',
  `dataId` int(11) NOT NULL DEFAULT '0',
  `dataRemarks` text,
  `scoreType` int(11) NOT NULL DEFAULT '0',
  `createTime` datetime NOT NULL,
  PRIMARY KEY (`scoreId`),
  KEY `userId` (`userId`),
  KEY `userId_2` (`userId`,`dataSrc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `users` (
  `userId` int(11) NOT NULL AUTO_INCREMENT,
  `loginName` varchar(20) NOT NULL,
  `loginSecret` int(11) NOT NULL,
  `loginPwd` varchar(50) NOT NULL,
  `userType` tinyint(4) NOT NULL DEFAULT '0',
  `userSex` tinyint(4) DEFAULT '0',
  `userName` varchar(100) DEFAULT NULL,
  `trueName` varchar(100) DEFAULT NULL,
  `brithday` date DEFAULT NULL,
  `userPhoto` varchar(200) DEFAULT NULL,
  `userQQ` varchar(20) DEFAULT NULL,
  `userPhone` char(11) DEFAULT '',
  `userEmail` varchar(50) DEFAULT '',
  `userScore` int(11) DEFAULT '0',
  `userTotalScore` int(11) DEFAULT '0',
  `lastIP` varchar(16) DEFAULT NULL,
  `lastTime` datetime DEFAULT NULL,
  `userFrom` tinyint(4) DEFAULT '0',
  `userMoney` decimal(11,2) DEFAULT '0.00',
  `lockMoney` decimal(11,2) DEFAULT '0.00',
  `userStatus` tinyint(4) NOT NULL DEFAULT '1',
  `dataFlag` tinyint(4) NOT NULL DEFAULT '1',
  `createTime` datetime NOT NULL,
  `payPwd` varchar(100) DEFAULT NULL,
  `rechargeMoney` decimal(11,2) DEFAULT '0.00' COMMENT '充值金额',
  `isInform` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`userId`),
  KEY `userStatus` (`userStatus`,`dataFlag`),
  KEY `loginName` (`loginName`),
  KEY `userPhone` (`userPhone`),
  KEY `userEmail` (`userEmail`),
  KEY `userType` (`userType`,`dataFlag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
