# 电商数仓

## 项目介绍：

1. 基于 传统电商的数据建立数据仓库
2. 数据生产：模拟产生用户行为数据 + 业务数据
3. 数据采集（包括用户行为数据采集以及业务数据采集）
4. 数仓构建（五层数仓 包括 ods层 + dwd层 +dws层 + dwt层 + ads层）

## 模块介绍：

1. 数据采集系统：

   用户行为数据采集：采用flume + kafka + flume + hadoop架构。对数据进行清洗，以及数据一致性的调整

   业务数据采集： 采用MySQL + sqoop + hadoop的架构。将前一天的数据传输到hdfs中进行保存

2. 数仓构建：

   ods层：数据的原始层，用于备份数据（雪花模型）

   dwd层：

   ​		--明细层：

   ​				将表区分为事实表，维度表等，进行维度退化，以及判断分类为 

   ​				事务性事实表、周期型快照事实表、累积性快照事实表等

   ​				不同的表有不同的导入策略

   ​		--dwd层的任务
   ​		① 对用户行为数据解析（用户行为初始都是json字符串，需要解析json，取出字段放入到hive表中）
   ​		② 对核心数据进行过滤
   ​		③ 对业务数据采用维度模型重新建模（星型模型）

   ​		-- dwd层维度退化
   ​			在dwd层业务数据的处理中对维度表进行退化，也就是把多个维度表合并为一个维度表，这样
   ​            将表减少，从而减少join操作（join操作在数仓中十分费时）。
   ​			这里将mysql中的数据导入到hive表中，没有了主键的定义，也没有范式的要求，所以可以进行
   ​			合并，有利于自己的后续计算操作。
   ​			比如： 将商品sku表，spu表，商品分类表（1，2，3级）合并为一个商品大表

   dws层：对数据进行轻度聚合，站在维度上看事实。

   ​				复杂数据类型定义
      			1) map结构
      			  map<string,string>
     			 2) array结构
   ​    			 array<string>
     			 3) struct结构
   ​    			 struct<id:int,name:string,age:int>
      			4) struct和array嵌套
     			   array<struct<id:int,name:string,age:int>>

   dwt层：对数据进一步聚合

   1. dwt: 在各维度(主题)上的 进一步聚合,累积时间(7天或一个月)下的度量值以及开头与结尾
      -- 宽表：
      	表有高表和宽表，在mysql中，按行存储数据，尽量设计为高表，可以加快效率，但是在olap中，按照列式存储，宽表可以发挥很大的作用。
      -- 宽表字段怎么来的？
      	维度关联的事实表度量值+<开头、结尾>+累积+累积一个时间段。

   2. - 设备主题宽表
        - old full outer join new
      - 会员主题宽表
        - old full outer join new
      - 商品主题宽表
        - old full outer join new
      - 活动主题宽表
        - old full outer join new
      - 地区主题宽表
        - old full outer join new

   ads层：业务层（根据业务，统计具体指标）

   1. 设备主题
      - 活跃设备数
        dwt_uv_topic
      - 每日新增设备
        dwt_uv_topic
      - 留存率

      - 沉默用户数
        沉默用户：只在安装当天启动过，且启动时间是在 7 天前
      - 本周回流用户数
        本周回流用户：上周未活跃，本周活跃的设备，且不是本周新增设备
      - 流失用户数
        流失用户：最近 7 天未活跃的设备
        从dwt_uv_topic获取
      - 最近连续三周活跃用户数
        dt卡在三周内就可以
      - 最近七天内连续三天活跃用户数
        - 将最近7天的数据按照用户id分组
        - 根据userid开窗排序
        - 将dt时间与rnk排名相减，得到新字段diff
        - group by userid，diff，去重，
        - having count(*) >3 寻找数量大于三的

   2. 会员主题
      - 会员信息

      - 漏斗分析
        - 统计“浏览首页->浏览商品详情页->加入购物车->下单->支付”的转化率
        - 思路:
          统计各个行为的人数，然后计算比值
        - 特点:
          方法一:
          	一个字段,一个字段统计,然后将两字段拼接,由于都是一行记录,所以直接按照一个相等的值(统计时间)进行拼接
          方法二:
          	- 将符合两字段的要求的数据全都筛选出来,然后按照mid分组,- 将其余需要的数据使用collect_set()封装为 Array
          	- 然后在外层使用 array_contains()进行统一处理判断.
          	- 代码:
          		select
          			'2020-06-14' dt,
          			sum(if(array_contains(pages,'home'),1,0)) home_count,
          			sum(if(array_contains(pages,'good_detail'),1,0)) good_detail_count
          		from
          			(
          			select
          				mid_id,
          				collect_set(page_id) pages
          			from dwd_page_log
          			where dt='2020-06-14' and page_id in ('home','good_detail')
          			group by mid_id
          			)tmp
          				

   3. 商品主题
      - 商品个数信息

      - 商品销量排名

      - 商品收藏排名

      - 商品加入购物车排名

      - 商品退款率排名（最近 30 天）

      - 商品差评率

   4. 营销主题(用户+商品+购买行为)
      -下单数目统计

      - 支付信息统计
        - 每日支付金额、支付人数、支付商品数、支付笔数以及下单到支付的平均时长（取自 DWD） 
        - 支付金额  dws_user_action_dayCount
        - 支付人数  dws_user_action_dayCount
        - 支付商品数 dws_sku_action_dayCount
        - 支付笔数 dws_user_action_dayCount
        - 下单到支付的平均时长 (时长,是累积型快照事实表)dwd_fact_order_info

   5. 地区主题
      - 地区主题信息
        - 从dwt层获取

   

   ​	

