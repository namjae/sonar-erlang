/*
 * Sonar Erlang Plugin
 * Copyright (C) 2012 Tamas Kende
 * kende.tamas@gmail.com
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
package org.sonar.plugins.erlang.dialyzer;

import org.hamcrest.Matchers;
import org.mockito.ArgumentCaptor;
import org.sonar.api.rules.Violation;

import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.rules.ActiveRule;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.erlang.ErlangPlugin;
import org.sonar.plugins.erlang.ProjectUtil;
import org.sonar.plugins.erlang.core.Erlang;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DialyzerSensorTest {

  private Configuration configuration;
  private Erlang erlang;
  private Project project;
  private SensorContext context;

  @Before
  public void setup() throws URISyntaxException, IOException {
    configuration = mock(Configuration.class);
    when(
        configuration.getString(ErlangPlugin.EUNIT_FOLDER_KEY,
            ErlangPlugin.EUNIT_DEFAULT_FOLDER)).thenReturn(
        ErlangPlugin.EUNIT_DEFAULT_FOLDER);
    erlang = new Erlang(configuration);
    context = mock(SensorContext.class);
    when(
        configuration.getString(ErlangPlugin.DIALYZER_FILENAME_KEY,
            ErlangPlugin.DIALYZER_DEFAULT_FILENAME)).thenReturn(
        ErlangPlugin.DIALYZER_DEFAULT_FILENAME);

    List<InputFile> srcFiles = new ArrayList<InputFile>();
    List<InputFile> otherFiles = new ArrayList<InputFile>();
    srcFiles.add(ProjectUtil
        .getInputFileByPath("/org/sonar/plugins/erlang/erlcount/.eunit/erlcount_lib.erl"));
    project = ProjectUtil.getProject(srcFiles, otherFiles, configuration);

    RulesProfile rp = mock(RulesProfile.class);
    ActiveRule activeRule = RuleUtil.generateActiveRule("unused_fun", "D019", null);
    when(rp.getActiveRule(DialyzerRuleRepository.REPOSITORY_KEY, "D019"))
        .thenReturn(activeRule);
    activeRule = RuleUtil.generateActiveRule("callback_missing", "D041", null);
    when(rp.getActiveRule(DialyzerRuleRepository.REPOSITORY_KEY, "D041"))
        .thenReturn(activeRule);

    File fileToAnalyse = new File(getClass().getResource(
        "/org/sonar/plugins/erlang/erlcount/src/erlcount_lib.erl").toURI());
    InputFile inputFile = InputFileUtils.create(fileToAnalyse.getParentFile(), fileToAnalyse);
    ArrayList<InputFile> inputFiles = new ArrayList<InputFile>();
    inputFiles.add(inputFile);

    new DialyzerSensor(erlang, rp).analyse(project, context);
  }

  @Test
  public void checkCoverSensor() throws URISyntaxException {
    ArgumentCaptor<Violation> argument = ArgumentCaptor.forClass(Violation.class);
    verify(context, times(3)).saveViolation(argument.capture());
    List<Violation> capturedViolations = argument.getAllValues();
    assertThat("violation is not D019", capturedViolations.get(0).getRule().getKey(), Matchers
        .equalTo("D019"));
    assertThat("violation is not D041", capturedViolations.get(1).getRule().getKey(), Matchers
        .equalTo("D041"));
    assertThat("violation is not D019", capturedViolations.get(2).getRule().getKey(), Matchers
        .equalTo("D019"));

  }

}