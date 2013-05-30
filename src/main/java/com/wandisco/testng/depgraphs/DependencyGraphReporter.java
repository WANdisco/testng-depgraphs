/*
 * Copyright 2013 WANdisco
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wandisco.testng.depgraphs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.xml.XmlSuite;

/**
 * TestNG reporting listener that generates a directed graph visualisation of test dependencies.
 * 
 * @author mbooth
 */
public class DependencyGraphReporter implements IReporter {

  private static final String OUTPUT_FILENAME = "dependency_graph";

  @Override
  public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
    for (ISuite suite : suites) {
      // Test methods
      Set<ITestNGMethod> skippedMethods = new HashSet<ITestNGMethod>();
      Set<ITestNGMethod> failedMethods = new HashSet<ITestNGMethod>();
      Set<ITestNGMethod> passedMethods = new HashSet<ITestNGMethod>();
      Set<ITestNGMethod> allMethods = new HashSet<ITestNGMethod>();

      // Test groups
      Map<String, Set<ITestNGMethod>> dependsOnGroups = new HashMap<String, Set<ITestNGMethod>>();
      Map<String, Set<ITestNGMethod>> groupsDependsOn = new HashMap<String, Set<ITestNGMethod>>();
      Set<String> allGroups = new HashSet<String>();

      // Process suite test results
      Collection<ISuiteResult> suiteResults = suite.getResults().values();
      for (ISuiteResult suiteResult : suiteResults) {
        ITestContext testCtx = suiteResult.getTestContext();
        skippedMethods.addAll(testCtx.getSkippedTests().getAllMethods());
        failedMethods.addAll(testCtx.getFailedTests().getAllMethods());
        passedMethods.addAll(testCtx.getPassedTests().getAllMethods());
        allMethods.addAll(Arrays.asList(testCtx.getAllTestMethods()));
      }
      for (ITestNGMethod method : allMethods) {
        for (String group : method.getGroupsDependedUpon()) {
          allGroups.add(group);
          Set<ITestNGMethod> groupMethods = dependsOnGroups.get(group);
          if (groupMethods == null) {
            groupMethods = new HashSet<ITestNGMethod>();
            dependsOnGroups.put(group, groupMethods);
          }
          groupMethods.add(method);
        }
        for (String group : method.getGroups()) {
          allGroups.add(group);
          Set<ITestNGMethod> groupMethods = groupsDependsOn.get(group);
          if (groupMethods == null) {
            groupMethods = new HashSet<ITestNGMethod>();
            groupsDependsOn.put(group, groupMethods);
          }
          groupMethods.add(method);
        }
      }

      // Draw a directed graph of the suite test results into a dot file
      BufferedWriter bw = null;
      File outputDot = new File(suite.getOutputDirectory(), OUTPUT_FILENAME + ".dot");
      try {
        // Start graph
        FileWriter fw = new FileWriter(outputDot);
        bw = new BufferedWriter(fw);
        bw.write("digraph G {");
        bw.newLine();
        bw.write("overlap = false");
        bw.newLine();

        // Output test nodes
        outputNodes(bw, skippedMethods, "yellow");
        outputNodes(bw, failedMethods, "red");
        outputNodes(bw, passedMethods, "green");

        // Output test relationships
        outputDeps(bw, skippedMethods);
        outputDeps(bw, failedMethods);
        outputDeps(bw, passedMethods);

        // Output group nodes and relationships
        for (String group : allGroups) {
          String groupName = "\"" + group + "\"";
          String subgraphName = "\"" + group + "_group\"";
          bw.write("subgraph " + subgraphName + " {");
          bw.newLine();
          bw.write(groupName + " [shape=box,color=blue,label=" + groupName + "]");
          bw.newLine();
          if (dependsOnGroups.get(group) != null) {
            for (ITestNGMethod method : dependsOnGroups.get(group)) {
              String node = method.getRealClass().getSimpleName() + "_" + method.getMethodName();
              bw.write(node + " -> " + groupName);
              bw.newLine();
            }
          }
          if (groupsDependsOn.get(group) != null) {
            for (ITestNGMethod method : groupsDependsOn.get(group)) {
              String node = method.getRealClass().getSimpleName() + "_" + method.getMethodName();
              bw.write(groupName + " -> " + node);
              bw.newLine();
            }
          }
          bw.write("}");
          bw.newLine();
        }

        // Finish graph
        bw.write("}");
        bw.newLine();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (bw != null) {
            bw.flush();
            bw.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      // Attempt to generate image from dot file
      File outputPng = new File(suite.getOutputDirectory(), OUTPUT_FILENAME + ".png");
      String[] args = new String[] { "dot", "-Tpng", "-o" + outputPng.getAbsolutePath(), outputDot.getAbsolutePath() };
      try {
        Runtime.getRuntime().exec(args);
      } catch (IOException e) {
        System.err.println("Error executing dot command: " + e.getMessage());
        StringBuilder command = new StringBuilder();
        for (String s : args) {
          if (command.length() > 0) {
            command.append(" ");
          }
          command.append(s);
        }
        System.err.println("Command was: " + command.toString());
      }
    }
  }

  private void outputNodes(final BufferedWriter bw, final Set<ITestNGMethod> methods, final String colour) throws IOException {
    for (ITestNGMethod method : methods) {
      String node = method.getRealClass().getSimpleName() + "_" + method.getMethodName();
      String nodeLabel = method.getRealClass().getSimpleName() + "\\n" + method.getMethodName();
      bw.write(node + " [shape=box,color=" + colour + ",style=rounded,label=\"" + nodeLabel + "\"]");
      bw.newLine();
    }
  }

  private void outputDeps(final BufferedWriter bw, final Set<ITestNGMethod> methods) throws IOException {
    for (ITestNGMethod method : methods) {
      String node = method.getRealClass().getSimpleName() + "_" + method.getMethodName();
      String[] deps = method.getMethodsDependedUpon();
      for (String dep : deps) {
        int lastdot = dep.lastIndexOf('.');
        dep = dep.substring(0, lastdot) + "_" + dep.substring(lastdot + 1);
        String nodeDep = dep.substring(dep.lastIndexOf('.') + 1);
        bw.write(node + " -> " + nodeDep);
        bw.newLine();
      }
    }
  }
}
