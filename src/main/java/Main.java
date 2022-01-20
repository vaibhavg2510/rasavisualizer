import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Node;
import org.apache.commons.exec.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static guru.nidi.graphviz.attribute.Attributes.attr;
import static guru.nidi.graphviz.attribute.Rank.RankDir.LEFT_TO_RIGHT;
import static guru.nidi.graphviz.model.Factory.*;

public class Main {
    static int nodeSeparatorInteger = 0;
    static int storyIdentifier = 0;

    public static void main(String[] args) {


        try {
            List<List<String>> fileLinesCore = getFileLines("/Users/vaibhavg/projects/yourstorypath");
            List<List<String>> fileLines = getConnectedFileLines(fileLinesCore,
                    Arrays.asList("/Users/vaibhavg/projects/commonConnectedFilesPath"));
            List<List<Node>> fileStoryList = getNodesFromLines(fileLines);
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
//            for (Node current : story) {
//                if (null == prev) {
//                    linkSourcesRasa.add(current);
//                } else {
//                    prev.link(current);
//                }
//                prev = current;
//            }

            }

            List<LinkSource> linkSources = Arrays.asList(
                    node("a").with(Color.GREEN).link(node("b"), node("c")),
                    node("b").with(Color.PURPLE).link((node("c"))));


            Graph g = graph("example2").directed()
//            .graphAttr().with(Rank.dir(LEFT_TO_RIGHT))
                    .linkAttr().with("class", "link-class")
                    .with(
                            linkSourcesRasa.toArray(new LinkSource[linkSourcesRasa.size()])
                    );

            Graphviz.fromGraph(g).render(Format.PNG).toFile(new File("example/frozenstuck.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

//    List<LinkSource> linkSources= Arrays.asList(
//                        node("a").with(Color.GREEN).link(node("b"), node("c")),
//                        node("b").with(Color.PURPLE).link((node("c"))));
//
//
//
//        Graph g = graph("example1").directed()
//            .graphAttr().with(Rank.dir(LEFT_TO_RIGHT))
//            .linkAttr().with("class", "link-class")
//            .with(
//                    linkSourcesRasa.toArray(new LinkSource[linkSourcesRasa.size()])
//            );
//
//        Graphviz.fromGraph(g).height(100).render(Format.PNG).toFile(new File("example/ex2.png"));


//    MutableGraph g = mutGraph("example1").setDirected(true).add(
//            mutNode("a").add(Color.RED).addLink(mutNode("b")));
//    Graphviz.fromGraph(g).width(200).render(Format.PNG).toFile(new File("example/ex1m.png"));

    }

    private static List<List<String>> getConnectedFileLines(List<List<String>> fileLinesCore, List<String> connectedFileList) {
        List<List<String>> connectedFileLines = new ArrayList<>();
        for (String absoluteFilePath : connectedFileList) {
            connectedFileLines.addAll(getFileLines(absoluteFilePath));
        }

//        List<String> checkpointsNeeded = getAllCheckpoints(fileLinesCore);
//
//        Map<String, List<String>> coreCheckpointToStoryMap = getCheckpointToStoryMap(fileLinesCore);
//        Map<String, List<String>> connectedFilesCheckpointToStoryMap = getCheckpointToStoryMap(connectedFileLines);
//        Map<String, List<String>> requiredConnectedCheckpointToStoryMap = new HashMap<>();
//
//        checkpointsNeeded.removeAll(coreCheckpointToStoryMap.keySet());

        List<List<String>> finalFileLines = new ArrayList<>();
        finalFileLines.addAll(fileLinesCore);
        finalFileLines.addAll(connectedFileLines);

//        int loopCount = 0;
//        while(!checkpointsNeeded.isEmpty() && loopCount < 100) {
//
//            ListIterator<String> iter = checkpointsNeeded.listIterator();
//            while(iter.hasNext()){
//                String checkpoint = iter.next();
//                if(connectedFilesCheckpointToStoryMap.containsKey(checkpoint)){
//                    checkpointsNeeded.addAll(getCheckpointsFromStory(connectedFilesCheckpointToStoryMap.get(checkpoint)));
//                    iter.remove();
//                }
//            }
//
//
//            loopCount+=1;
//        }
//        int loopCount = 0;
//        while(!checkpointsNeeded.isEmpty() && loopCount < 100) {
//            List<String> newCheckpointsNeeded = new ArrayList<>(checkpointsNeeded);
//            for (String checkpoint: checkpointsNeeded) {
//                if(connectedFilesCheckpointToStoryMap.containsKey(checkpoint)){
//                    newCheckpointsNeeded.addAll(getCheckpointsFromStory(connectedFilesCheckpointToStoryMap.get(checkpoint)));
//                    coreCheckpointToStoryMap.put(checkpoint, connectedFilesCheckpointToStoryMap.get(checkpoint));
//                    finalFileLines.add(connectedFilesCheckpointToStoryMap.get(checkpoint));
//                }
//            }
//            newCheckpointsNeeded.removeAll(coreCheckpointToStoryMap.keySet());
//            checkpointsNeeded = newCheckpointsNeeded;
//            loopCount+=1;
//        }

        
        

        return finalFileLines;
    }

    private static Map<String, List<String>> getCheckpointToStoryMap(List<List<String>> fileLines) {
        Map<String, List<String>> checkpointToStoryMap= new HashMap<>();
        for (List<String> story : fileLines) {
            if (story.get(1).startsWith(">")) {
                checkpointToStoryMap.put(story.get(1), story);
            }
        }
        return checkpointToStoryMap;
    }

    private static List<String> getAllCheckpoints(List<List<String>> fileLinesCore) {
        List<String> checkpoints = new ArrayList<>();
        for (List<String> story : fileLinesCore) {
            checkpoints.addAll(getCheckpointsFromStory(story));
        }
        return (checkpoints);
    }

    private static List<String> getCheckpointsFromStory(List<String> story) {
        List<String> checkpoints = new ArrayList<>();
        for (String line : story) {
            if (line.startsWith(">")) {
                checkpoints.add(line);
            }
        }
        return checkpoints;
    }

    private static List<List<Node>> getNodesFromLines(List<List<String>> fileLines) {
        List<List<Node>> nodesList = new ArrayList<>();

        for (List<String> story : fileLines) {
            List<Node> currentList = new ArrayList<>();
            for (String line : story) {
                if (line.startsWith("##")) {
                    currentList.add(node(line).with(Color.PURPLE));
                } else if ((line.startsWith(">"))) {
                    currentList.add(node(line).with(Color.GREEN));
                } else
                    currentList.add(node(line));
            }
            nodesList.add(currentList);
        }
        return nodesList;
    }

    private static List<List<String>> getFileLines(String coreStoryFileAbsolutePath) {
        List<List<String>> fileLines = new ArrayList<>();
        try {
            Scanner s = new Scanner(new FileInputStream(coreStoryFileAbsolutePath));
            List<String> currentList = new ArrayList<>();
            while (s.hasNext()) {
                String line = s.nextLine();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("##")) {
                    storyIdentifier += 1;
                    line += " " + storyIdentifier;
                } else if (!line.startsWith(">")) {
                    line += " " + storyIdentifier + "_" + nodeSeparatorInteger;
                    nodeSeparatorInteger++;
                }
                if (line.startsWith("##")) {
                    if (!currentList.isEmpty()) {
                        fileLines.add(currentList);
                    }
                    currentList = new ArrayList<>();
                }
                if (!line.startsWith("##"))
                    currentList.add(line);
            }
            fileLines.add(currentList);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return fileLines;
    }

}
