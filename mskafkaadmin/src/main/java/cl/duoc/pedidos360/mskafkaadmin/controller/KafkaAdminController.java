package cl.duoc.pedidos360.mskafkaadmin.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.mskafkaadmin.dto.CreateTopicRequest;
import cl.duoc.pedidos360.mskafkaadmin.dto.UpdateConfigsRequest;
import cl.duoc.pedidos360.mskafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/kafka")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    @GetMapping("/cluster")
    public Map<String, Object> describeCluster() throws Exception {
        return service.describeCluster();
    }

    @GetMapping("/topics")
    public List<String> listTopics() throws Exception {
        return service.listTopics();
    }

    @GetMapping("/topics/{name}")
    public Map<String, Object> describeTopic(
            @PathVariable("name") String name) throws Exception {

        return service.describeTopic(name);
    }

    @PostMapping("/topics")
    public ResponseEntity<Map<String, Object>> createTopic(
            @Valid @RequestBody CreateTopicRequest request)
            throws Exception {

        Map<String, Object> result = service.createTopic(request);

        URI location = URI.create(
                "/kafka/topics/" + request.name()
        );

        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/topics/{name}/configs")
    public List<Map<String, Object>> getTopicConfigs(
            @PathVariable("name") String name) throws Exception {

        return service.getTopicConfigs(name);
    }

    @PatchMapping("/topics/{name}/configs")
    public Map<String, Object> updateTopicConfigs(
            @PathVariable("name") String name,
            @Valid @RequestBody UpdateConfigsRequest request)
            throws Exception {

        return service.updateTopicConfigs(
                name,
                request.configs()
        );
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Void> deleteTopic(
            @PathVariable("name") String name) throws Exception {

        service.deleteTopic(name);

        return ResponseEntity.noContent().build();
    }
}