package oml.StarTopologyAPI.sites;

import oml.StarTopologyAPI.GenericProxy;
import oml.StarTopologyAPI.GenericWrapper;
import oml.StarTopologyAPI.network.Network;

import java.util.Map;

public abstract class BipartiteNetwork implements Network {

    Map<Integer, GenericWrapper> sites; // A map containing all the remote sites

    Map<Integer, GenericProxy> hubs; // A map containing all the hubs

}
