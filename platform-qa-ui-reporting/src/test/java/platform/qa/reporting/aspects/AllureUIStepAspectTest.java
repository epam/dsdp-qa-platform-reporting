package platform.qa.reporting.aspects;

import static org.assertj.core.api.Assertions.assertThat;
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

class AllureUIStepAspectTest {

  private AllureUIStepAspect aspect;
  private StepAspect rpAspectMock;

  @BeforeEach
  void setUp() throws Exception {
    aspect = new AllureUIStepAspect();
    rpAspectMock = mock(StepAspect.class);

    Field field = AllureUIStepAspect.class.getDeclaredField("rpStepAspect");
    field.setAccessible(true);
    field.set(aspect, rpAspectMock);
  }

  @Test
  void shouldStartNestedStep() {
    JoinPoint joinPoint = mock(JoinPoint.class);
    io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
    when(allureStep.value()).thenReturn("UI step start");

    aspect.startNestedStep(joinPoint, allureStep);

    ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
    verify(rpAspectMock).startNestedStep(eq(joinPoint), stepCaptor.capture());

    Step rpStep = stepCaptor.getValue();
    assertThat(rpStep.value()).isEqualTo("UI step start");
    assertThat(rpStep.isIgnored()).isFalse();
  }

  @Test
  void shouldFinishNestedStep() {
    io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
    when(allureStep.value()).thenReturn("UI step finish");

    aspect.finishNestedStep(allureStep);

    ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
    verify(rpAspectMock).finishNestedStep(stepCaptor.capture());

    assertThat(stepCaptor.getValue().value()).isEqualTo("UI step finish");
  }

  @Test
  void shouldFailNestedStep() {
    io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
    when(allureStep.value()).thenReturn("UI step failed");
    Throwable throwable = new RuntimeException("boom");

    aspect.failedNestedStep(allureStep, throwable);

    ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
    verify(rpAspectMock).failedNestedStep(stepCaptor.capture(), eq(throwable));

    Step rpStep = stepCaptor.getValue();
    assertThat(rpStep.value()).isEqualTo("UI step failed");
    assertThat(rpStep.isIgnored()).isFalse();
  }

  @Test
  void shouldProvideTemplateConfig() {
    io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
    when(allureStep.value()).thenReturn("Template config step");

    aspect.finishNestedStep(allureStep);

    ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
    verify(rpAspectMock).finishNestedStep(stepCaptor.capture());

    TemplateConfig config = stepCaptor.getValue().config();

    assertThat(config.methodNameTemplate()).isEqualTo("method");
    assertThat(config.selfNameTemplate()).isEqualTo("selfName");
    assertThat(config.fieldDelimiter()).isEqualTo(", ");
    assertThat(config.iterableStartSymbol()).isEqualTo("[");
    assertThat(config.iterableEndSymbol()).isEqualTo("]");
    assertThat(config.arrayStartSymbol()).isEqualTo("{");
    assertThat(config.arrayEndSymbol()).isEqualTo("}");
    assertThat(config.classRefTemplate()).isEqualTo("class");
    assertThat(config.classNameTemplate()).isEqualTo("className");
  }

  @Test
  void rpStepAnnotationTypeShouldBeCorrect() {
    io.qameta.allure.Step allureStep = mock(io.qameta.allure.Step.class);
    when(allureStep.value()).thenReturn("Annotation type step");

    aspect.finishNestedStep(allureStep);

    ArgumentCaptor<Step> stepCaptor = ArgumentCaptor.forClass(Step.class);
    verify(rpAspectMock).finishNestedStep(stepCaptor.capture());

    Annotation annotation = stepCaptor.getValue();
    assertThat(annotation.annotationType()).isEqualTo(com.epam.reportportal.annotations.Step.class);
  }
}
