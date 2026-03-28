package platform.qa.reporting.aspects;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.annotations.TemplateConfig;
import com.epam.reportportal.aspect.StepAspect;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AllureAPIStepAspectTest {

    private AllureAPIStepAspect aspect;
    private StepAspect rpAspectMock;

    @BeforeEach
    void setUp() throws Exception {
        aspect = new AllureAPIStepAspect();
        rpAspectMock = mock(StepAspect.class);

        Field field = AllureAPIStepAspect.class.getDeclaredField("rpStepAspect");
        field.setAccessible(true);
        field.set(aspect, rpAspectMock);
    }

    @Test
    void shouldStartNestedStep() {
        JoinPoint joinPoint = mock(JoinPoint.class);
        io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
        when(allureStep.value()).thenReturn("My step");

        aspect.startNestedStep(joinPoint, allureStep);

        ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
        verify(rpAspectMock).startNestedStep(eq(joinPoint), stepCaptor.capture());

        Step rpStep = stepCaptor.getValue();
        assertThat(rpStep.value()).isEqualTo("My step");
        assertThat(rpStep.isIgnored()).isFalse();
    }

    @Test
    void shouldFinishNestedStep() {
        io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
        when(allureStep.value()).thenReturn("Finish step");

        aspect.finishNestedStep(allureStep);

        ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
        verify(rpAspectMock).finishNestedStep(stepCaptor.capture());

        assertThat(stepCaptor.getValue().value()).isEqualTo("Finish step");
    }

    @Test
    void shouldFailNestedStep() {
        io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
        Throwable throwable = new RuntimeException("boom");

        aspect.failedNestedStep(allureStep, throwable);

        ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
        verify(rpAspectMock).failedNestedStep(stepCaptor.capture(), eq(throwable));

        assertThat(stepCaptor.getValue().isIgnored()).isFalse();
    }

    @Test
    void shouldGenerateTemplateConfig() {
        io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
        when(allureStep.value()).thenReturn("Template test");

        aspect.finishNestedStep(allureStep);

        ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
        verify(rpAspectMock).finishNestedStep(stepCaptor.capture());

        TemplateConfig config = stepCaptor.getValue().config();

        assertThat(config.methodNameTemplate()).isEqualTo("method");
        assertThat(config.selfNameTemplate()).isEqualTo("selfName");
        assertThat(config.iterableStartSymbol()).isEqualTo("[");
        assertThat(config.arrayEndSymbol()).isEqualTo("}");
        assertThat(config.classNameTemplate()).isEqualTo("className");
    }

    @Test
    void rpStepAnnotationTypeShouldBeCorrect() {
        io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
        when(allureStep.value()).thenReturn("Annotation test");

        aspect.finishNestedStep(allureStep);

        ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
        verify(rpAspectMock).finishNestedStep(stepCaptor.capture());

        Annotation annotation = stepCaptor.getValue();
        assertThat(annotation.annotationType())
                .isEqualTo(com.epam.reportportal.annotations.Step.class);
    }
}
