# Spark Reverse Proxy
This applications work like load balancer for cluster of Spark-Thrift server. It listen all requests from client connected and forward each request to target server, which has minimum {activeJobs}. Numbers of {activeJobs} are collected via calls API provided by Apache Spark.
