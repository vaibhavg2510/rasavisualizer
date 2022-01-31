import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;

public class Main {
    static final String CHECKPOINT_IDENTIFIER = ">";
    static final String STORY_IDENTIFIER = "##";

    static int nodeSeparatorInteger = 0;
    static int storyIdentifier = 0;

    public static void main(String[] args) {

        List<String> coreFilePathList = Arrays.asList(
                "/Users/vaibhavg/projects/project-name/coreFilePath.md"
        );

        List<String> connectedFilePathList = Arrays.asList(
                "/Users/vaibhavg/projects/project-name/connectedFilePath1.md",
                "/Users/vaibhavg/projects/project-name/connectedFilePath2.md"
        );

        String outputFilePath = "outputfiles/temp";

        String diffIdentifier = "+";

        plotGraph(coreFilePathList, connectedFilePathList, outputFilePath, true, diffIdentifier);
        plotGraph(coreFilePathList, connectedFilePathList, outputFilePath, false, diffIdentifier);


    }

    private static void plotGraph(List<String> coreFilePathList, List<String> connectedFilePathList, String outputFilePath, boolean withStoryTitle, String diffIdentifier) {
        try {
            List<List<String>> fileLinesCore = getFileLines(coreFilePathList, withStoryTitle);
            List<List<String>> fileLinesConnected = getFileLines(connectedFilePathList, withStoryTitle);
            Map<String, List<List<String>>> fileLinesMap = getConnectedFileLines(fileLinesCore, fileLinesConnected, withStoryTitle);

            plotGraph(fileLinesMap, outputFilePath, diffIdentifier, withStoryTitle);

        } catch (Exception e) {
            System.out.println("Exception while creating graph " + e.getMessage());
        }
    }

    private static void plotGraph(Map<String, List<List<String>>> fileLinesMap, String outputFilePath, String diffIdentifier, boolean withStoryTitle) throws IOException {
        List<List<Node>> fileStoryList = getNodesFromLines(fileLinesMap, diffIdentifier);
        List<LinkSource> linkSourcesRasa = getLinkSources(fileStoryList);

        Graph g = graph("rasastory").directed()
                .linkAttr().with("class", "link-class")
                .with(
                        linkSourcesRasa.toArray(new LinkSource[0])
                );
        outputFilePath += withStoryTitle ? "_withStory" : "_withoutStory";
        Graphviz.fromGraph(g).render(Format.PNG).toFile(new File(outputFilePath));
    }

    private static List<LinkSource> getLinkSources(List<List<Node>> fileStoryList) {
        List<LinkSource> linkSourcesRasa = new ArrayList<>();
        for (List<Node> story : fileStoryList) {
            Node prev = story.get(story.size() - 1);
            for (int i = story.size() - 2; i >= 0; i--) {
                Node current = story.get(i);
                current = current.link(prev);
                if (i == 0) {
                    linkSourcesRasa.add(current);
                } else {
                    prev = current;
                }
            }

        }
        return linkSourcesRasa;
    }

    private static Map<String, List<List<String>>> getConnectedFileLines(List<List<String>> fileLinesCore, List<List<String>> connectedFileLines, boolean withStoryTitle) {

        Set<String> allCheckpointsNeeded = getAllCheckpoints(fileLinesCore);

        Map<String, List<List<String>>> finalizedCheckpointToStoryMap = getCheckpointToListOfStoryMap(fileLinesCore, withStoryTitle);
        Map<String, List<List<String>>> connectedFilesCheckpointToStoryListMap = getCheckpointToListOfStoryMap(connectedFileLines, withStoryTitle);

        List<List<String>> connectedFileLinesFinal = new ArrayList<>();

        int loopCount = 0;
        while (!allCheckpointsNeeded.isEmpty() && loopCount < 100) {
            allCheckpointsNeeded.removeAll(finalizedCheckpointToStoryMap.keySet());

            List<String> pendingCheckpoints = new ArrayList<>(allCheckpointsNeeded);

            for (String checkpoint : pendingCheckpoints) {
                if (connectedFilesCheckpointToStoryListMap.containsKey(checkpoint)) {
                    allCheckpointsNeeded.addAll(getAllCheckpoints(connectedFilesCheckpointToStoryListMap.get(checkpoint)));
                    finalizedCheckpointToStoryMap.put(checkpoint, connectedFilesCheckpointToStoryListMap.get(checkpoint));
                    connectedFileLinesFinal.addAll(connectedFilesCheckpointToStoryListMap.get(checkpoint));
                }
            }

            loopCount += 1;
        }

        Map<String, List<List<String>>> finalFileLinesMap = new LinkedHashMap<>();
        finalFileLinesMap.put("core", fileLinesCore);
        finalFileLinesMap.put("connected", connectedFileLinesFinal);
        return finalFileLinesMap;
    }

    private static Map<String, List<List<String>>> getCheckpointToListOfStoryMap(List<List<String>> fileLines, boolean withStoryTitle) {
        Map<String, List<List<String>>> checkpointToStoryListMap = new HashMap<>();
        for (List<String> story : fileLines) {
            String storyFirstLine = withStoryTitle ? story.get(1) : story.get(0);
            if (storyFirstLine.startsWith(CHECKPOINT_IDENTIFIER)) {
                List<List<String>> storyList = checkpointToStoryListMap.getOrDefault(storyFirstLine, new ArrayList<>());
                storyList.add(story);
                checkpointToStoryListMap.put(storyFirstLine, storyList);
            }
        }
        return checkpointToStoryListMap;
    }

    private static Set<String> getAllCheckpoints(List<List<String>> fileLinesCore) {
        HashSet<String> checkpoints = new HashSet<>();
        for (List<String> story : fileLinesCore) {
            checkpoints.addAll(getCheckpointsFromStory(story));
        }
        return (checkpoints);
    }

    private static List<String> getCheckpointsFromStory(List<String> story) {
        List<String> checkpoints = new ArrayList<>();
        for (String line : story) {
            if (line.startsWith(CHECKPOINT_IDENTIFIER)) {
                checkpoints.add(line);
            }
        }
        return checkpoints;
    }

    private static List<List<Node>> getNodesFromLines(Map<String, List<List<String>>> fileLinesMap, String diffIdentifier) {
        List<List<Node>> nodesList = new ArrayList<>();
        nodesList.addAll(getNodesFromLines(fileLinesMap.get("core"), diffIdentifier, Color.BLACK));
        nodesList.addAll(getNodesFromLines(fileLinesMap.get("connected"), diffIdentifier, Color.SKYBLUE));
        return nodesList;
    }

    private static List<List<Node>> getNodesFromLines(List<List<String>> fileLinesCore, String diffIdentifier, Color defaultColor) {
        List<List<Node>> nodesList = new ArrayList<>();
        for (List<String> story : fileLinesCore) {
            List<Node> currentList = new ArrayList<>();
            for (String line : story) {
                if (line.startsWith(STORY_IDENTIFIER)) {
                    currentList.add(node(line).with(Color.PURPLE));
                } else if ((line.startsWith(CHECKPOINT_IDENTIFIER))) {
                    currentList.add(node(line).with(Color.GREEN));
                } else if ((line.startsWith(diffIdentifier))) {
                    currentList.add(node(line.substring(diffIdentifier.length())).with(Color.RED));
                } else
                    currentList.add(node(line).with(defaultColor));
            }
            nodesList.add(currentList);
        }
        return nodesList;
    }

    private static List<List<String>> getFileLines(List<String> storyFileAbsolutePathList, boolean withStoryTitle) {
        List<List<String>> fileLinesList = new ArrayList<>();
        if (null == storyFileAbsolutePathList) {
            return new ArrayList<>();
        }
        for (String absoluteFilePath : storyFileAbsolutePathList) {
            fileLinesList.addAll(getFileLines(absoluteFilePath, withStoryTitle));
        }
        return fileLinesList;
    }

    private static List<List<String>> getFileLines(String storyFileAbsolutePath, boolean withStoryTitle) {
        List<List<String>> fileLines = new ArrayList<>();
        try {
            Scanner s = new Scanner(new FileInputStream(storyFileAbsolutePath));
            List<String> currentList = new ArrayList<>();
            while (s.hasNext()) {
                String line = s.nextLine();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith(STORY_IDENTIFIER)) {
                    storyIdentifier += 1;
                    line += " " + storyIdentifier;
                } else if (!line.startsWith(CHECKPOINT_IDENTIFIER)) {
                    line += " " + storyIdentifier + "_" + nodeSeparatorInteger;
                    nodeSeparatorInteger++;
                }
                if (line.startsWith(STORY_IDENTIFIER)) {
                    if (!currentList.isEmpty()) {
                        fileLines.add(currentList);
                    }
                    currentList = new ArrayList<>();
                }
                if (withStoryTitle || !line.startsWith(STORY_IDENTIFIER))      //add all lines if with story title, otherwise add only iff not starting with ##
                    currentList.add(line);
            }
            fileLines.add(currentList);
        } catch (FileNotFoundException e) {
            System.out.println("Exception while reading file " + storyFileAbsolutePath + e.getMessage());
        }
        return fileLines;
    }

}
