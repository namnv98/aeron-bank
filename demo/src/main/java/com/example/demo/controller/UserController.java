package com.example.demo.controller;


import java.io.StringWriter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.spring.VelocityEngineUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

  @Autowired
  private VelocityEngine velocityEngine;

  @PostMapping(value = "/demo")
  @ResponseBody
  public ResponseEntity<Object> forgotPassword() {
    var a = VelocityEngineUtils.mergeTemplateIntoString(velocityEngine, "templates/email.vm",
      Map.of("url", "localhost:9090"));

    VelocityContext context = new VelocityContext();
    context.put("url", "httls://localhost:8080/resetpass?token=123123");

    Template template = velocityEngine.getTemplate("demo.vm");

    StringWriter writer = new StringWriter();
    template.merge(context, writer);

    // Output the result
    System.out.println(writer.toString());  // Output the processed templat

    return null;
  }

}
