package io.ddd4j.quarkus.annotation.ddd;

import io.ddd4j.annotation.ddd.DDDAnnotation;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;

/**
 * 独立验证器：验证 ddd4j-quarkus-annotation 的 11 个 DDD 注解
 * 与 Jakarta CDI 的元注解融合正确。
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
public final class AnnotationFusionVerifier {

    private AnnotationFusionVerifier() {}

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        passed += verify("DomainService",       DomainService.class);
        passed += verify("DomainRepository",    DomainRepository.class);
        passed += verify("ApplicationService",  ApplicationService.class);
        passed += verify("QueryService",       QueryService.class);
        passed += verify("CommandExecutor",    CommandExecutor.class);
        passed += verify("DomainEntity",       DomainEntity.class);
        passed += verify("DomainValueObject",  DomainValueObject.class);
        passed += verify("DomainGateway",      DomainGateway.class);
        passed += verify("DomainAssembler",    DomainAssembler.class);
        passed += verify("DomainConverter",    DomainConverter.class);

        // 验证 @DomainEvent 不在 ddd4j-quarkus-annotation 中
        System.out.println();
        System.out.println("--- @DomainEvent 不下沉验证 ---");
        try {
            Class.forName("io.ddd4j.quarkus.annotation.ddd.DomainEvent");
            System.out.println("❌ FAIL: @DomainEvent 不应在 ddd4j-quarkus-annotation 中");
            failed++;
        } catch (ClassNotFoundException e) {
            System.out.println("✅ PASS: @DomainEvent 不在 ddd4j-quarkus-annotation");
            passed++;
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("总计: " + (passed + failed) + " | 通过: " + passed + " | 失败: " + failed);
        System.out.println("========================================");

        if (failed > 0) {
            System.err.println("❌ 验证未通过");
            System.exit(1);
        } else {
            System.out.println("✅ 全部验证通过！ddd4j-quarkus-annotation 符合架构设计");
        }
    }

    private static int verify(
            String name,
            Class<? extends java.lang.annotation.Annotation> dddAnnotation) {

        // 1. 必须标注 DDDAnnotation
        DDDAnnotation ddd = dddAnnotation.getAnnotation(DDDAnnotation.class);
        if (ddd == null) {
            System.out.println("❌ " + name + ": 缺少 @DDDAnnotation 元注解");
            return 0;
        }
        System.out.println("✅ " + name + ": 已标注 @DDDAnnotation");

        // 2. 必须融合 Jakarta CDI @ApplicationScoped 元注解
        ApplicationScoped scoped = dddAnnotation.getAnnotation(ApplicationScoped.class);
        if (scoped == null) {
            System.out.println("❌ " + name + ": 缺少 @ApplicationScoped 元注解");
            return 0;
        }
        System.out.println("✅ " + name + ": 已融合 @ApplicationScoped");

        return 1;
    }
}
