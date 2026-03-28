package platform.qa.reporting.aspects;

import java.lang.annotation.Annotation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import com.epam.reportportal.annotations.TemplateConfig;
import com.epam.reportportal.aspect.StepAspect;

@Aspect
public class AllureUIStepAspect {
    private final StepAspect rpStepAspect = new StepAspect();

    public AllureUIStepAspect() {
    }

    @Pointcut("@annotation(step)")
    public void withStepAnnotation(io.qameta.allure.Step step) {
    }

    @Pointcut("execution(* *.*(..))")
    public void anyMethod() {
    }

    @Before(
            value = "anyMethod() && withStepAnnotation(step)",
            argNames = "joinPoint,step"
    )
    public void startNestedStep(JoinPoint joinPoint, io.qameta.allure.Step step) {
        this.rpStepAspect.startNestedStep(joinPoint, this.generateRpInstance(step));
    }

    @AfterReturning(
            value = "anyMethod() && withStepAnnotation(step)",
            argNames = "step"
    )
    public void finishNestedStep(io.qameta.allure.Step step) {
        this.rpStepAspect.finishNestedStep(this.generateRpInstance(step));
    }

    @AfterThrowing(
            value = "anyMethod() && withStepAnnotation(step)",
            throwing = "throwable",
            argNames = "step,throwable"
    )
    public void failedNestedStep(io.qameta.allure.Step step, Throwable throwable) {
        this.rpStepAspect.failedNestedStep(this.generateRpInstance(step), throwable);
    }

    private com.epam.reportportal.annotations.Step generateRpInstance(io.qameta.allure.Step allureStep) {
        return this.getRpStepFromAllure(allureStep);
    }

    private com.epam.reportportal.annotations.Step getRpStepFromAllure(final io.qameta.allure.Step allureStep) {
        return new com.epam.reportportal.annotations.Step() {
            public Class<? extends Annotation> annotationType() {
                return com.epam.reportportal.annotations.Step.class;
            }

            public String value() {
                return allureStep.value();
            }

            public String description() {
                return "";
            }

            public boolean isIgnored() {
                return false;
            }

            @Override
            public TemplateConfig config() {
                return AllureUIStepAspect.this.getTemplateConfig();
            }
        };
    }

    private TemplateConfig getTemplateConfig() {
        return new TemplateConfig() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return TemplateConfig.class;
            }

            @Override
            public String methodNameTemplate() {
                return "method";
            }

            @Override
            public String selfNameTemplate() {
                return "selfName";
            }

            @Override
            public String fieldDelimiter() {
                return ", ";
            }

            @Override
            public String iterableStartSymbol() {
                return "[";
            }

            @Override
            public String iterableEndSymbol() {
                return "]";
            }

            @Override
            public String iterableElementDelimiter() {
                return ", ";
            }

            @Override
            public String arrayStartSymbol() {
                return "{";
            }

            @Override
            public String arrayEndSymbol() {
                return "}";
            }

            @Override
            public String arrayElementDelimiter() {
                return ", ";
            }

            @Override
            public String classRefTemplate() {
                return "class";
            }

            @Override
            public String classNameTemplate() {
                return "className";
            }
        };
    }
}
