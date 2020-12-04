# 环境准备

## seata

下载地址：http://seata.io/en-us/blog/download.html

选择binary，笔者的版本是1.4.0。

后面seata客户端的版本要与seata-server匹配。

seata中有两个重要配置文件file.conf与registry.conf，在服务端（seata-server）与客户端（需要分布式事务的微服务）都需要，具体下文说明。

## nacos

官网：https://nacos.io/zh-cn/docs/quick-start.html

windows选择zip，版本选择近期的，笔者的是1.3.2

# seata registry.conf

seata/conf/registry.conf 给seata-server添加注册中心和配置中心

```conf
# seata-server的注册中心
registry {
  type = "nacos"
  loadBalance = "RandomLoadBalance"
  loadBalanceVirtualNodes = 10

  nacos {
    application = "seata-server"
    serverAddr = "127.0.0.1:8848"
    namespace = ""
    cluster = "default"
    username = ""
    password = ""
  }
  
}

# seata-server的配置中心
config {
  type = "nacos"

  nacos {
    serverAddr = "127.0.0.1:8848"
    namespace = ""
    group = "SEATA_GROUP"
    username = ""
    password = ""
  }
}

```

# seata file.conf

1、registy.conf中config.type=file，或registry.type=file，才加载file.conf中的配置参数，否则无需file.conf。

2、当config.type选择了file以外的类型，比如nacos，则需要把seate的配置提前推送到nacos的配置中心。

# seata配置推送nacos

1、将seata配置做成config.txt文件

```txt
transport.type=TCP
transport.server=NIO
transport.heartbeat=true
transport.enableClientBatchSendRequest=false
transport.threadFactory.bossThreadPrefix=NettyBoss
transport.threadFactory.workerThreadPrefix=NettyServerNIOWorker
transport.threadFactory.serverExecutorThreadPrefix=NettyServerBizHandler
transport.threadFactory.shareBossWorker=false
transport.threadFactory.clientSelectorThreadPrefix=NettyClientSelector
transport.threadFactory.clientSelectorThreadSize=1
transport.threadFactory.clientWorkerThreadPrefix=NettyClientWorkerThread
transport.threadFactory.bossThreadSize=1
transport.threadFactory.workerThreadSize=default
transport.shutdown.wait=3
service.vgroupMapping.seata-storage-service-group=default
service.vgroupMapping.seata-order-service-group=default
service.vgroupMapping.seata-account-service-group=default
service.default.grouplist=127.0.0.1:8091
service.enableDegrade=false
service.disableGlobalTransaction=false
client.rm.asyncCommitBufferLimit=10000
client.rm.lock.retryInterval=10
client.rm.lock.retryTimes=30
client.rm.lock.retryPolicyBranchRollbackOnConflict=true
client.rm.reportRetryCount=5
client.rm.tableMetaCheckEnable=false
client.rm.sqlParserType=druid
client.rm.reportSuccessEnable=false
client.rm.sagaBranchRegisterEnable=false
client.tm.commitRetryCount=5
client.tm.rollbackRetryCount=5
store.mode=db
store.file.dir=file_store/data
store.file.maxBranchSessionSize=16384
store.file.maxGlobalSessionSize=512
store.file.fileWriteBufferCacheSize=16384
store.file.flushDiskMode=async
store.file.sessionReloadReadSize=100
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.jdbc.Driver
store.db.url=jdbc:mysql://127.0.0.1:3306/seata
store.db.user=root
store.db.password=accp
store.db.minConn=5
store.db.maxConn=30
store.db.globalTable=global_table
store.db.branchTable=branch_table
store.db.queryLimit=100
store.db.lockTable=lock_table
store.db.maxWait=5000
server.recovery.committingRetryPeriod=1000
server.recovery.asynCommittingRetryPeriod=1000
server.recovery.rollbackingRetryPeriod=1000
server.recovery.timeoutRetryPeriod=1000
server.maxCommitRetryTimeout=-1
server.maxRollbackRetryTimeout=-1
server.rollbackRetryTimeoutUnlockEnable=false
client.undo.dataValidation=true
client.undo.logSerialization=jackson
server.undo.logSaveDays=7
server.undo.logDeletePeriod=86400000
client.undo.logTable=undo_log
client.log.exceptionRate=100
transport.serialization=seata
transport.compressor=none
metrics.enabled=false
metrics.registryType=compact
metrics.exporterList=prometheus
metrics.exporterPrometheusPort=9898
```

2、推送脚本nacos-config.sh

可以在seata的GitHub项目中找到对应的文件：https://github.com/seata/seata/tree/1.2.0/script/config-center

```sh
#!/usr/bin/env bash
# Copyright 1999-2019 Seata.io Group.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at、
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
 
while getopts ":h:p:g:t:" opt
do
  case $opt in
  h)
    host=$OPTARG
    ;;
  p)
    port=$OPTARG
    ;;
  g)
    group=$OPTARG
    ;;
  t)
    tenant=$OPTARG
    ;;
  ?)
    echo " USAGE OPTION: $0 [-h host] [-p port] [-g group] [-t tenant] "
    exit 1
    ;;
  esac
done
 
if [[ -z ${host} ]]; then
    host=localhost
fi
if [[ -z ${port} ]]; then
    port=8848
fi
if [[ -z ${group} ]]; then
    group="SEATA_GROUP"
fi
if [[ -z ${tenant} ]]; then
    tenant=""
fi
 
nacosAddr=$host:$port
contentType="content-type:application/json;charset=UTF-8"
 
echo "set nacosAddr=$nacosAddr"
echo "set group=$group"
 
failCount=0
tempLog=$(mktemp -u)
function addConfig() {
  curl -X POST -H "${1}" "http://$2/nacos/v1/cs/configs?dataId=$3&group=$group&content=$4&tenant=$tenant" >"${tempLog}" 2>/dev/null
  if [[ -z $(cat "${tempLog}") ]]; then
    echo " Please check the cluster status. "
    exit 1
  fi
  if [[ $(cat "${tempLog}") =~ "true" ]]; then
    echo "Set $3=$4 successfully "
  else
    echo "Set $3=$4 failure "
    (( failCount++ ))
  fi
}
 
count=0
for line in $(cat $(dirname "$PWD")/config.txt | sed s/[[:space:]]//g); do
  (( count++ ))
	key=${line%%=*}
  value=${line#*=}
	addConfig "${contentType}" "${nacosAddr}" "${key}" "${value}"
done
 
echo "========================================================================="
echo " Complete initialization parameters,  total-count:$count ,  failure-count:$failCount "
echo "========================================================================="
 
if [[ ${failCount} -eq 0 ]]; then
	echo " Init nacos config finished, please start seata-server. "
else
	echo " init nacos config fail. "
fi
```

配置中比较重要的就是事务分组和store，store后续讲到。

事务分组配置指的是呼应TC集群（seata-server），这里都默认default

service.vgroupMapping.seata-storage-service-group=default
service.vgroupMapping.seata-order-service-group=default
service.vgroupMapping.seata-account-service-group=default

各微服务通过以下配置，来读取这个“default”

```yml
seata:
  tx-service-group: ${spring.application.name}-group
```



3、启动nacos

cmd进入nacos/bin目录，输入以下命令

```
start startup.cmd -m standalone
```

standalone代表着单机模式运行，非集群模式

弹出新的cmd窗口，待完全完成后，即可访问http://127.0.0.1:8848/nacos/index.html

默认的账号密码就是：**nacos/nacos** 

4、推送命令

```
 sh nacos-config.sh localhost
```

推送完成后，在nacos配置列表查看是否成功

**参数说明：**

**-h: host，默认值 localhost**

**-p: port，默认值 8848**

**-g: 配置分组，默认值为 'SEATA_GROUP'**

**-t: 租户信息，对应 Nacos 的命名空间ID字段, 默认值为空 ''**

# seata事务日志库

推送到nacos的seata配置比较重要的是事务日志存储模式store，有file、db、redis多种，这里选择db

```
store.mode=db
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.jdbc.Driver
store.db.url=jdbc:mysql://127.0.0.1:3306/seata
store.db.user=root
store.db.password=accp
store.db.minConn=5
store.db.maxConn=30
store.db.globalTable=global_table
store.db.branchTable=branch_table
store.db.queryLimit=100
store.db.lockTable=lock_table
store.db.maxWait=5000
```

当然如果配置用的是file.conf，也可以进行如下相关配置

seata/conf/ flie.conf 

```conf
store {
  mode = "db"

  db {
    datasource = "druid"
    dbType = "mysql"
    driverClassName = "com.mysql.jdbc.Driver"
    url = "jdbc:mysql://127.0.0.1:3306/seata"
    user = "root"
    password = "accp"
    minConn = 5
    maxConn = 30
    globalTable = "global_table"
    branchTable = "branch_table"
    lockTable = "lock_table"
    queryLimit = 100
    maxWait = 5000
  }
}
```



如果选择db，我们需要创建一个seata数据库，建表sql在seata项目的github找到，

地址：<https://github.com/seata/seata/tree/1.2.0/script/server/db> 目录 mysql.sql 中

```sql
CREATE TABLE IF NOT EXISTS `global_table`
(
    `xid`                       VARCHAR(128) NOT NULL,
    `transaction_id`            BIGINT,
    `status`                    TINYINT      NOT NULL,
    `application_id`            VARCHAR(32),
    `transaction_service_group` VARCHAR(32),
    `transaction_name`          VARCHAR(128),
    `timeout`                   INT,
    `begin_time`                BIGINT,
    `application_data`          VARCHAR(2000),
    `gmt_create`                DATETIME,
    `gmt_modified`              DATETIME,
    PRIMARY KEY (`xid`),
    KEY `idx_gmt_modified_status` (`gmt_modified`, `status`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

-- the table to store BranchSession data
CREATE TABLE IF NOT EXISTS `branch_table`
(
    `branch_id`         BIGINT       NOT NULL,
    `xid`               VARCHAR(128) NOT NULL,
    `transaction_id`    BIGINT,
    `resource_group_id` VARCHAR(32),
    `resource_id`       VARCHAR(256),
    `branch_type`       VARCHAR(8),
    `status`            TINYINT,
    `client_id`         VARCHAR(64),
    `application_data`  VARCHAR(2000),
    `gmt_create`        DATETIME(6),
    `gmt_modified`      DATETIME(6),
    PRIMARY KEY (`branch_id`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

-- the table to store lock data
CREATE TABLE IF NOT EXISTS `lock_table`
(
    `row_key`        VARCHAR(128) NOT NULL,
    `xid`            VARCHAR(96),
    `transaction_id` BIGINT,
    `branch_id`      BIGINT       NOT NULL,
    `resource_id`    VARCHAR(256),
    `table_name`     VARCHAR(32),
    `pk`             VARCHAR(36),
    `gmt_create`     DATETIME,
    `gmt_modified`   DATETIME,
    PRIMARY KEY (`row_key`),
    KEY `idx_branch_id` (`branch_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
```

mysql版本不同可能会执行错误，笔者实测需要把	“DATETIME(6)”	改成	“DATETIME”

# 业务库

## 订单库order

```sql
CREATE DATABASE seata_order;

USE seata_order;

CREATE TABLE `order` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(11) DEFAULT NULL COMMENT '用户id',
  `product_id` bigint(11) DEFAULT NULL COMMENT '产品id',
  `count` int(11) DEFAULT NULL COMMENT '数量',
  `money` decimal(11,0) DEFAULT NULL COMMENT '金额',
  `status` int(1) DEFAULT NULL COMMENT '订单状态：0：创建中；1：已完结',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;订单库order
```

## 库存库storage

```sql
CREATE DATABASE seata_storage;

USE seata_storage;

CREATE TABLE `storage` (
                         `id` bigint(11) NOT NULL AUTO_INCREMENT,
                         `product_id` bigint(11) DEFAULT NULL COMMENT '产品id',
                         `total` int(11) DEFAULT NULL COMMENT '总库存',
                         `used` int(11) DEFAULT NULL COMMENT '已用库存',
                         `residue` int(11) DEFAULT NULL COMMENT '剩余库存',
                         PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

INSERT INTO `seata_storage`.`storage` (`id`, `product_id`, `total`, `used`, `residue`) VALUES ('1', '1', '100', '0', '100');
```



## 账户库account

```sql
CREATE DATABASE seata_account;

USE seata_account;


CREATE TABLE `account` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint(11) DEFAULT NULL COMMENT '用户id',
  `total` decimal(10,0) DEFAULT NULL COMMENT '总额度',
  `used` decimal(10,0) DEFAULT NULL COMMENT '已用余额',
  `residue` decimal(10,0) DEFAULT '0' COMMENT '剩余可用额度',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

INSERT INTO `seata_account`.`account` (`id`, `user_id`, `total`, `used`, `residue`) VALUES ('1', '1', '1000', '0', '1000');
```

## undo_log

在三个库中都插入undo_log表，sql地址：<https://github.com/seata/seata/blob/develop/script/client/at/db/mysql.sql>

```sql
-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT(20)   NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(100) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8 COMMENT ='AT transaction mode undo table';
```

mysql版本不同可能会执行错误，笔者实测需要把	“DATETIME(6)”	改成	“DATETIME”

# 工程架构

1、order,storage,account的parent指向seata-transaction-demo；

2、common则是单独模块，提供公用实体类与API；

3、本项目目的是以最简单方式演示分布式事务，很多分布式模块能省则省；

4、每个微服务，作为seata的客户端都需要上文说到的registry.conf和file.conf两个配置文件，放在resource下，不过本项目演示都整合到yml中去了，客户端不再需要这两个文件。

# 验证分布式事务

1、启动Nacos，然后启动Seata服务端

（1）nacos启动

cmd进入nacos/bin目录，输入以下命令

```
start startup.cmd -m standalone
```

standalone代表着单机模式运行，非集群模式

弹出新的cmd窗口，待完全完成后，即可访问http://127.0.0.1:8848/nacos/index.html

默认的账号密码就是：**nacos/nacos** 

（2）seata启动

cmd进入seata\bin，然后输入以下命令

```
.\seata-server.bat -p 8091 -h 127.0.0.1 -m db
```

2、分别启动order，storage，account服务

3、查看Seata服务端控制台输出内容：三个服务分别注册了 RM 和 TM，都用通道连接

4、浏览器访问地址：http://localhost:9011/order/create?userId=1&productId=1&count=10&money=100

5、查看数据库，order，storage，account，三个数据库数据的变化

6、重新调用修改参数money=50，触发Account服务中的模拟业务失败，http://localhost:9011/order/create?userId=1&productId=1&count=10&money=50，

7、请求报错，数据无变化，符合正常逻辑，验证Seata分布式事务管理已生效

8、还可以去掉Order服务中的@GlobalTransactional注解，然后重新启动Order服务。当服务报错时，查看数据库，三个数据库的数据异常，不符合正常逻辑

# 排错与注意

1、客户端（需要分布式事务的微服务）定时报错：no available service 'default' found

注意seata客户端版本与服务端匹配，本项目演示seata服务为1.4.0，客户端版本spring-cloud-starter-alibaba-seata为2.2.2.RELEASE

2、初次调用找不到服务：多试几次，第一次容易找不到

3、由于用了nacos和seata两个中间件，里面都有分组的概念，不要混淆了

4、seata-server在nacos注册，默认分组“SEATA_GROUP”，这个分组是属于nacos分组。同一个nacos分组的微服务才能互相调用，笔者这里实测seata-server可以和其他微服务不同分组，但在微服务yml的配置文件中要显示指出seata-server的分组。

# 关于seata事务分组

​	seata事务分组就是像service.vgroupMapping.seata-account-service-group=default，带有“vgroupMapping”的配置，这里的default对应的是TC集群（seata-server），这里都默认default

Q：我有10个微服务，那我要分10个组吗 ？

A：分组的含义就是映射到一套集群，所以你可以配一个分组也可以配置多个。如果你其他的微服务有独立发起事务可以配置多个，如果只是作为服务调用方参与事务那么没必要配置多个。

Q：不知道分组的目的是什么？那不管什么情况我始终就一个分组有没问题？

A：没问题，分组是用于资源的逻辑隔离，多租户的概念。

Q：是不是一个事务中所有的微服务都必须是同一组才行？

A：没有这个要求的。但是不同的分组需要映射到同一个集群上。

# 关于nacos分组

1、同一个分组的微服务才能互相调用

seata-server可以和其他微服务不同分组，但在微服务yml的配置文件中要显示指出seata-server的分组。

2、同一个分组才能读取配置

同样微服务yml的配置文件中要显示指出seata-server的分组后，微服务DEFAULT_GROUP也可读取SEATA_GROUP分组的配置