package br.com.ideiageni.uct;

import java.util.List;

/**
 * Created by ariel on 20/06/2016.
 */
public class DX80Nodes {
    final int numRegXNode = 16;

    private String[] names;
    private int[] id;

    private boolean[] readEnable;

    public DX80Nodes(int numNodes) {
        initialize(numNodes);
    }

    public DX80Nodes(int numNodes, String names[]) {
        initialize(numNodes);
        this.names = names;
    }

    private void initialize (int numNodes){
        id = new int[numNodes];
        readEnable = new boolean[numNodes];
        names = new String[numNodes];

        for(int i=0;i<numNodes;i++){
            id[i] = i;
            readEnable[i] = false;
            if(i==0) names[i] = "GW";
            else names[i] = String.valueOf(i);
        }

    }

    public int getValue(int node, int reg, int[] values) {
        return values[node*16+reg+1];
    }

    public String[] getValuesStr(int[] values) {
        String[] valuesStr = new String[numRegXNode];
        int i = 0;

        for (Object intValues  : values) {
            valuesStr[i] = (String) intValues;
            i++;
        }

        return valuesStr;
    }

    public String[] getNodeStr(int node, int[] values) {
        String[] nodeStr = new String[numRegXNode + 1];
        nodeStr[0] = names[node];

        for (int i = node*16+1;i<(node+1)*16+1;i++) {
            nodeStr[i] = String.valueOf(values[i]);
        }
        return nodeStr;
    }

    public boolean isReadEnable(int node) {
        return readEnable[node];
    }

    public boolean[] getReadEnable() {
        return readEnable;
    }

    public void setReadEnable(int node, boolean readEnable) {
        this.readEnable[node] = readEnable;
    }

    public String getName(int node) {
        return this.names[node];
    }

    public String[] getNames() {
        return this.names;
    }

    public String setName(int node, String nome) {
        return this.names[node] = nome;
    }

}


