{
    "name" : "pifunctional",
    "replicationClass" : "SimpleStrategy",
    "replicationFactors" : {
        "replication_factor" : 1
    },
    "definitions" : [
        {
            "name": "object1",
            "fields": [
                {"name": "account_id", "type": "uuid"},
                {"name": "user_id", "type": "uuid"},
                {"name": "field1", "type": "varchar"}
            ],
            "indexes" : [
                {
                    "key": "account_id:user_id",
                    "shardingStrategy": {"type": "ShardingStrategyNone"}
                }
            ]
        },
        {
            "name": "object2",
            "fields": [
                {"name": "account_id", "type": "uuid"},
                {"name": "user_id", "type": "uuid"},
                {"name": "field2", "type": "varchar"}
            ],
            "indexes" : [
                {
                    "key": "account_id:user_id",
                    "shardingStrategy": {"type": "ShardingStrategyNone"}
                }
            ]
        }
    ]
}


