package cl.duoc.pedidos360.mskafkaadmin.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.mskafkaadmin.dto.CreateTopicRequest;

@Service
public class KafkaAdminService {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaAdminService.class);

    private final AdminClient adminClient;
    private final TopicPolicy topicPolicy;

    public KafkaAdminService(
            AdminClient adminClient,
            TopicPolicy topicPolicy) {

        this.adminClient = adminClient;
        this.topicPolicy = topicPolicy;
    }

    private <T> T await(KafkaFuture<T> future) throws Exception {

        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
    }

    public Map<String, Object> describeCluster() throws Exception {

        var result = adminClient.describeCluster();

        var brokers = await(result.nodes())
                .stream()
                .map(node -> Map.<String, Object>of(
                        "id", node.id(),
                        "host", node.host(),
                        "port", node.port()
                ))
                .toList();

        var controller = await(result.controller());

        return Map.of(
                "clusterId", await(result.clusterId()),
                "controllerId", controller == null ? -1 : controller.id(),
                "brokers", brokers
        );
    }

    public List<String> listTopics() throws Exception {

        return await(adminClient.listTopics().names())
                .stream()
                .sorted()
                .toList();
    }

    public Map<String, Object> describeTopic(String name)
            throws Exception {

        topicPolicy.validateName(name);

        var descriptions = await(
                adminClient.describeTopics(List.of(name)).allTopicNames()
        );

        var topic = descriptions.get(name);

        var partitions = topic.partitions()
                .stream()
                .map(partition -> Map.<String, Object>of(
                        "partition", partition.partition(),
                        "leader", partition.leader() == null
                                ? -1
                                : partition.leader().id(),
                        "replicas", partition.replicas()
                                .stream()
                                .map(node -> node.id())
                                .toList(),
                        "isr", partition.isr()
                                .stream()
                                .map(node -> node.id())
                                .toList()
                ))
                .toList();

        return Map.of(
                "name", topic.name(),
                "internal", topic.isInternal(),
                "partitionCount", partitions.size(),
                "partitions", partitions
        );
    }

    public Map<String, Object> createTopic(CreateTopicRequest request)
            throws Exception {

        topicPolicy.validateWritable(request.name());
        topicPolicy.validateConfigs(request.configs());

        if (request.configs() != null
                && request.configs().containsKey("min.insync.replicas")) {

            int minIsr = Integer.parseInt(
                    request.configs().get("min.insync.replicas")
            );

            if (minIsr > request.replicationFactor()) {
                throw new IllegalArgumentException(
                        "min.insync.replicas no puede superar el factor de replicación"
                );
            }
        }

        NewTopic topic = new NewTopic(
                request.name(),
                request.partitions().intValue(),
                request.replicationFactor().shortValue()
        );

        if (request.configs() != null) {
            topic.configs(request.configs());
        }

        await(adminClient.createTopics(List.of(topic)).all());

        log.info(
                "Tópico creado: {} particiones={} réplicas={}",
                request.name(),
                request.partitions(),
                request.replicationFactor()
        );

        return Map.of(
                "name", request.name(),
                "partitions", request.partitions(),
                "replicationFactor", request.replicationFactor(),
                "message", "Creación confirmada; los metadatos pueden tardar en propagarse"
        );
    }

    public List<Map<String, Object>> getTopicConfigs(String name)
            throws Exception {

        topicPolicy.validateName(name);

        ConfigResource resource = new ConfigResource(
                ConfigResource.Type.TOPIC,
                name
        );

        var configurations = await(
                adminClient.describeConfigs(List.of(resource)).all()
        );

        var configuration = configurations.get(resource);

        return configuration.entries()
                .stream()
                .sorted(Comparator.comparing(ConfigEntry::name))
                .map(entry -> {

                    Map<String, Object> row = new LinkedHashMap<>();

                    row.put("name", entry.name());
                    row.put(
                            "value",
                            entry.isSensitive() ? null : entry.value()
                    );
                    row.put("source", entry.source().name());
                    row.put("readOnly", entry.isReadOnly());

                    return row;
                })
                .toList();
    }

    public Map<String, Object> updateTopicConfigs(
            String name,
            Map<String, String> configs) throws Exception {

        topicPolicy.validateWritable(name);
        topicPolicy.validateConfigs(configs);

        var topic = await(
                adminClient.describeTopics(List.of(name)).allTopicNames()
        ).get(name);

        if (configs.containsKey("min.insync.replicas")) {

            int minIsr = Integer.parseInt(
                    configs.get("min.insync.replicas")
            );

            boolean exceedsReplication = topic.partitions()
                    .stream()
                    .anyMatch(partition ->
                            minIsr > partition.replicas().size()
                    );

            if (exceedsReplication) {
                throw new IllegalArgumentException(
                        "min.insync.replicas supera las réplicas de alguna partición"
                );
            }
        }

        ConfigResource resource = new ConfigResource(
                ConfigResource.Type.TOPIC,
                name
        );

        Collection<AlterConfigOp> operations = configs.entrySet()
                .stream()
                .map(entry -> new AlterConfigOp(
                        new ConfigEntry(
                                entry.getKey(),
                                entry.getValue()
                        ),
                        AlterConfigOp.OpType.SET
                ))
                .toList();

        await(
                adminClient.incrementalAlterConfigs(
                        Map.of(resource, operations)
                ).all()
        );

        log.info(
                "Configuración actualizada: tópico={} propiedades={}",
                name,
                configs.keySet()
        );

        return Map.of(
                "name", name,
                "updated", configs
        );
    }

    public void deleteTopic(String name) throws Exception {

        topicPolicy.validateWritable(name);

        await(adminClient.deleteTopics(List.of(name)).all());

        log.info("Eliminación de tópico confirmada: {}", name);
    }
}