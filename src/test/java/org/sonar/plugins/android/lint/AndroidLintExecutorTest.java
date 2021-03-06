/*
 * SonarQube Android Plugin
 * Copyright (C) 2013 SonarSource and Jerome Van Der Linden, Stephane Nicolas, Florian Roncari, Thomas Bores
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.android.lint;

import com.android.tools.lint.detector.api.Severity;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AndroidLintExecutorTest {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  private AndroidLintExecutor executor;
  private RulesProfile rulesProfile;
  private ModuleFileSystem fs;

  @Before
  public void prepare() throws Exception {
    fs = mock(ModuleFileSystem.class);
    rulesProfile = mock(RulesProfile.class);
    ProjectClasspath projectClasspath = mock(ProjectClasspath.class);
    RuleFinder ruleFinder = mock(RuleFinder.class);
    executor = new AndroidLintExecutor(ruleFinder, fs, rulesProfile, projectClasspath, new PathResolver());
    when(fs.baseDir()).thenReturn(new File(this.getClass().getResource("/HelloWorld").toURI()));
    when(fs.sourceDirs()).thenReturn(Arrays.asList(new File(this.getClass().getResource("/HelloWorld/src").toURI())));
    when(fs.binaryDirs()).thenReturn(Arrays.asList(new File(this.getClass().getResource("/HelloWorld/bin").toURI())));
    when(projectClasspath.getElements()).thenReturn(Arrays.asList(new File(this.getClass().getResource("/HelloWorld/bin").toURI())));
    ActiveRule activeRule = mock(ActiveRule.class);
    when(rulesProfile.getActiveRule(eq(AndroidLintConstants.REPOSITORY_KEY), anyString())).thenReturn(activeRule);
    Rule rule = Rule.create(AndroidLintConstants.REPOSITORY_KEY, "foo");
    when(ruleFinder.findByKey(eq(AndroidLintConstants.REPOSITORY_KEY), anyString())).thenReturn(rule);
  }

  @Test
  public void lintExecutionTest() {
    SensorContext sensorContext = mock(SensorContext.class);
    when(sensorContext.getResource(any(Resource.class))).thenReturn(new JavaFile("foo"));
    executor.execute(sensorContext);

    verify(sensorContext, times(22)).saveViolation(any(Violation.class));
  }

  @Test
  public void shouldNotCreateViolationWhenRuleIsDisabled() {
    when(rulesProfile.getActiveRule(eq(AndroidLintConstants.REPOSITORY_KEY), anyString())).thenReturn(null);

    SensorContext sensorContext = mock(SensorContext.class);
    when(sensorContext.getResource(any(Resource.class))).thenReturn(new JavaFile("foo"));
    executor.execute(sensorContext);

    verify(sensorContext, never()).saveViolation(any(Violation.class));
  }

  @Test
  public void testSonarExclusions() {
    SensorContext sensorContext = mock(SensorContext.class);
    when(sensorContext.getResource(any(Resource.class))).thenReturn(null).thenReturn(new JavaFile("foo"));
    executor.execute(sensorContext);

    verify(sensorContext, times(21)).saveViolation(any(Violation.class));
  }

  @Test
  public void shouldRequireCompiledSources() throws Exception {
    when(fs.binaryDirs()).thenReturn(Arrays.asList(new File("/not/exist")));

    SensorContext sensorContext = mock(SensorContext.class);
    when(sensorContext.getResource(any(Resource.class))).thenReturn(new JavaFile("foo"));

    thrown.expect(SonarException.class);
    thrown.expectMessage("Android Lint needs sources to be compiled.");
    executor.execute(sensorContext);
  }

  @Test
  public void testLog() {
    executor.log(Severity.ERROR, null, "Something %s", "arg");
    executor.log(Severity.FATAL, null, "Something %s", "arg");
    executor.log(Severity.IGNORE, null, "Something %s", "arg");
    executor.log(Severity.INFORMATIONAL, new SonarException(), "Something %s", "arg");
    executor.log(Severity.WARNING, null, "Something %s", "arg");
  }
}
