package org.itxtech.nemisys;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Cleanup;
import lombok.Getter;
import org.itxtech.nemisys.command.Command;
import org.itxtech.nemisys.command.CommandSender;
import org.itxtech.nemisys.command.data.CommandDataVersions;
import org.itxtech.nemisys.event.TextContainer;
import org.itxtech.nemisys.event.TranslationContainer;
import org.itxtech.nemisys.event.player.PlayerChatEvent;
import org.itxtech.nemisys.event.player.PlayerLoginEvent;
import org.itxtech.nemisys.event.player.PlayerLogoutEvent;
import org.itxtech.nemisys.event.player.PlayerTransferEvent;
import org.itxtech.nemisys.multiversion.ProtocolGroup;
import org.itxtech.nemisys.network.RakNetInterface;
import org.itxtech.nemisys.network.SourceInterface;
import org.itxtech.nemisys.network.protocol.mcpe.*;
import org.itxtech.nemisys.network.protocol.mcpe.types.ScoreInfo;
import org.itxtech.nemisys.network.protocol.spp.PlayerLoginPacket;
import org.itxtech.nemisys.network.protocol.spp.PlayerLogoutPacket;
import org.itxtech.nemisys.network.protocol.spp.RedirectPacket;
import org.itxtech.nemisys.permission.PermissibleBase;
import org.itxtech.nemisys.permission.Permission;
import org.itxtech.nemisys.permission.PermissionAttachment;
import org.itxtech.nemisys.permission.PermissionAttachmentInfo;
import org.itxtech.nemisys.plugin.Plugin;
import org.itxtech.nemisys.utils.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Author: PeratX
 * Nemisys Project
 */
public class Player implements CommandSender {

    public static final boolean TRANSFER_SCREEN = true;

    public boolean closed;
    @Getter
    protected UUID uuid;
    private byte[] cachedLoginPacket = new byte[0];
    @Getter
    private String name;
    @Getter
    private String ip;
    @Getter
    private int port;
    @Getter
    private long clientId;
    @Getter
    private long randomClientId;
    @Getter
    private int protocol = -1;
    @Getter
    private ProtocolGroup protocolGroup;
    private SourceInterface interfaz;
    @Getter
    private Client client;
    @Getter
    private Server server;
    @Getter
    private byte[] rawUUID;
    private boolean isFirstTimeLogin = true;
    private long lastUpdate;
    @Getter
    private Skin skin;
    @Getter
    private ClientChainData loginChainData;

    protected Map<String, CommandDataVersions> clientCommands;

    protected LongOpenHashSet spawnedEntities = new LongOpenHashSet();
    protected Set<UUID> playerList = new HashSet<>();

    protected final Queue<DataPacket> incomingPackets = new ConcurrentLinkedQueue<>();
    protected final Queue<DataPacket> outgoingPackets = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean ticking = new AtomicBoolean();

    private PermissibleBase perm = null;

    protected final int raknetProtocol;

    protected final Map<String, Set<Long>> scoreboards = new HashMap<>();

    public Player(SourceInterface interfaz, long clientId, String ip, int port) {
        this.interfaz = interfaz;
        this.clientId = clientId;
        this.ip = ip;
        this.port = port;
        this.name = "";
        this.server = Server.getInstance();
        this.lastUpdate = System.currentTimeMillis();
        this.perm = new PermissibleBase(this);

        if (interfaz instanceof RakNetInterface) {
            this.raknetProtocol = ((RakNetInterface) interfaz).raknet.getSessionManager().getSession(ip, port).getProtocol();
        } else {
            this.raknetProtocol = 0;
        }
        if (this.raknetProtocol >= 9) {
            this.protocolGroup = ProtocolGroup.PROTOCOL_16;
        }
    }

    public void handleDataPacket(DataPacket packet) {
        try {
            if (this.closed) {
                return;
            }
            this.lastUpdate = System.currentTimeMillis();

            if (packet instanceof BatchPacket) {
                this.getServer().getNetwork().processBatch((BatchPacket) packet, this);
                return;
            }

            if (packet.supports(getProtocolGroup())) {

        /*if(!(packet instanceof GenericPacket)) {
            MainLogger.getLogger().info("FROM PLAYER: " + packet.getClass().getSimpleName());
        }*/

                if(Nemisys.DEEP_DEBUG) {
                    MainLogger.getLogger().info("PLAYER HANDLE PACKET: "+packet.getClass().getSimpleName());
                }

                switch (packet.pid()) {
                    case ProtocolInfo.LOGIN_PACKET:
                        LoginPacket loginPacket = (LoginPacket) packet;
                        this.cachedLoginPacket = loginPacket.cacheBuffer;
                        this.skin = loginPacket.skin;
                        this.name = loginPacket.username;
                        this.uuid = loginPacket.clientUUID;
                        if (this.uuid == null) {
                            this.close(TextFormat.RED + "Please choose another name and try again!");
                            break;
                        }
                        this.rawUUID = Binary.writeUUID(this.uuid);
                        this.randomClientId = loginPacket.clientId;
                        this.protocol = loginPacket.protocol;
                        this.protocolGroup = ProtocolGroup.from(this.protocol);
                        this.loginChainData = ClientChainData.read(loginPacket);

                        this.server.getLogger().info(this.getServer().getLanguage().translateString("nemisys.player.logIn", TextFormat.AQUA + this.name + TextFormat.WHITE,
                                this.ip,
                                String.valueOf(this.port),
                                "" + TextFormat.GREEN + this.getRandomClientId() + TextFormat.WHITE));

                        Map<String, Client> c = this.server.getMainClients();

                        String clientHash;
                        if (c.size() > 0) {
                            clientHash = new ArrayList<>(c.keySet()).get(new Random().nextInt(c.size()));
                        } else {
                            clientHash = "";
                        }

                        PlayerLoginEvent ev;
                        this.server.getPluginManager().callEvent(ev = new PlayerLoginEvent(this, "Plugin Reason", clientHash));
                        if (ev.isCancelled()) {
                            this.close(ev.getKickMessage());
                            break;
                        }
                        if (this.server.getMaxPlayers() <= this.server.getOnlinePlayers().size()) {
                            this.close("Synapse Server: " + TextFormat.RED + "Synapse server is full!");
                            break;
                        }
                        if (ev.getClientHash() == null || ev.getClientHash().equals("")) {
                            this.close("Synapse Server: " + TextFormat.RED + "No target server!");
                            break;
                        }
                        if (!this.server.getClients().containsKey(ev.getClientHash())) {
                            this.close("Synapse Server: " + TextFormat.RED + "Target server is not online!");
                            break;
                        }

                        this.transfer(this.server.getClients().get(ev.getClientHash()));
                        return;
                    case ProtocolInfo.COMMAND_REQUEST_PACKET:
                        CommandRequestPacket commandRequestPacket = (CommandRequestPacket) packet;

                /*PlayerCommandPreprocessEvent playerCommandPreprocessEvent = new PlayerCommandPreprocessEvent(this, commandRequestPacket.command);
                this.server.getPluginManager().callEvent(playerCommandPreprocessEvent);
                if (playerCommandPreprocessEvent.isCancelled()) {
                    break;
                }*/

                        if (this.server.dispatchCommand(this, commandRequestPacket.command.substring(1), false))
                            return;
                        //System.out.println("HANDLED COMMAND: "+commandRequestPacket.command);
                        break;
                    case ProtocolInfo.TEXT_PACKET:
                        TextPacket textPacket = (TextPacket) packet;

                        if (textPacket.type == TextPacket.TYPE_CHAT) {
                            PlayerChatEvent chatEvent = new PlayerChatEvent(this, textPacket.message);
                            getServer().getPluginManager().callEvent(chatEvent);

                            if (chatEvent.isCancelled()) {
                                return;
                            }
                        }
                        break;
                }
            }
        } catch (Throwable t) {
            MainLogger.getLogger().error("Exception happened while handling outgoing packet " + packet.getClass().getSimpleName(), t);
        }

        if (this.client != null) this.redirectPacket(packet.getBuffer());
    }

    protected void handleIncomingPacket(DataPacket pk) {
        /*if (this.protocolGroup.ordinal() == ProtocolGroup.PROTOCOL_11.ordinal()) {
            this.sendDataPacket(pk);
            return;
        }*/

        if (pk instanceof BatchPacket) {
            processIncomingBatch((BatchPacket) pk);
            return;
        }

        try {

            if (pk.supports(getProtocolGroup()) && !(pk instanceof GenericPacket)) {

                //MainLogger.getLogger().info("FROM SERVER: " + pk.getClass().getSimpleName() + ": 0x" + Integer.toHexString(pk.getBuffer()[0]));
                Long entityId = null;

                switch (pk.pid()) {
                    case ProtocolInfo.ADD_PLAYER_PACKET:
                        entityId = ((AddPlayerPacket) pk).entityRuntimeId;
                        //MainLogger.getLogger().info(getName()+"  add player "+entityId);
                        break;
                    case ProtocolInfo.ADD_ENTITY_PACKET:
                        entityId = ((AddEntityPacket) pk).entityRuntimeId;
                        //MainLogger.getLogger().info(getName()+"  add entity "+entityId);
                        break;
                    case ProtocolInfo.ADD_ITEM_ENTITY_PACKET:
                        entityId = ((AddItemEntityPacket) pk).entityRuntimeId;
                        //MainLogger.getLogger().info(getName()+"  add item "+entityId);
                        break;
                    case ProtocolInfo.ADD_PAINTING_PACKET:
                        entityId = ((AddPaintingPacket) pk).entityRuntimeId;
                        //MainLogger.getLogger().info(getName()+"  add paintning "+entityId);
                        break;
                    case ProtocolInfo.REMOVE_ENTITY_PACKET:
                        spawnedEntities.remove(((RemoveEntityPacket) pk).eid);
                        //MainLogger.getLogger().info(getName()+"  REMOVE ENTITY "+((RemoveEntityPacket) pk).eid);
                        break;
                    case ProtocolInfo.PLAYER_LIST_PACKET:
                        PlayerListPacket playerListPacket = (PlayerListPacket) pk;

                        if (playerListPacket.type == PlayerListPacket.TYPE_ADD) {
                            playerList.addAll(Arrays.stream(playerListPacket.entries).map((e) -> e.uuid).collect(Collectors.toList()));
                        } else {
                            playerList.removeAll(Arrays.stream(playerListPacket.entries).map((e) -> e.uuid).collect(Collectors.toList()));
                        }
                        break;
                    case ProtocolInfo.AVAILABLE_COMMANDS_PACKET:
                        AvailableCommandsPacket commandsPacket = (AvailableCommandsPacket) pk;

                        if (this.protocolGroup.ordinal() >= ProtocolGroup.PROTOCOL_12.ordinal()) {
                            this.clientCommands = new HashMap<>(commandsPacket.commands);
                            sendCommandData();

                            //System.out.println("HADNLED COMMANDS LIST PACKET");
                            return;
                        }

                        break;
                    case ProtocolInfo.SET_DISPLAY_OBJECTIVE_PACKET:
                        SetDisplayObjectivePacket sdop = (SetDisplayObjectivePacket) pk;

                        scoreboards.putIfAbsent(sdop.objective, new HashSet<>());
                        break;
                    case ProtocolInfo.REMOVE_OBJECTIVE_PACKET:
                        RemoveObjectivePacket rop = (RemoveObjectivePacket) pk;

                        break;
                    case ProtocolInfo.SET_SCORE_PACKET:
                        SetScorePacket ssp = (SetScorePacket) pk;

                        SetScorePacket.Action act = ssp.action;
                        ssp.infos.forEach(info -> {
                            scoreboards.compute(info.objective, (k, v) -> {
                                if (v == null) {
                                    v = new HashSet<>();
                                }

                                if (act == SetScorePacket.Action.SET) {
                                    v.add(info.scoreId);
                                } else {
                                    v.remove(info.scoreId);
                                }
                                return v;
                            });
                        });

                        break;
                }

                if (entityId != null) {
                    spawnedEntities.add(entityId.longValue());
                }
            }
        } catch (Throwable t) {
            MainLogger.getLogger().error("Exception happened while handling incoming packet " + pk.getClass().getSimpleName(), t);
        }

        this.sendDataPacket(pk);
    }

    public void redirectPacket(byte[] buffer) {
        RedirectPacket pk = new RedirectPacket();
        pk.protocol = this.protocol;
        pk.uuid = this.uuid;
        pk.direct = false;
        pk.mcpeBuffer = buffer;
        this.client.sendDataPacket(pk);
    }

    public void onUpdate(long currentTick) {
        ticking.set(true);

        while (!outgoingPackets.isEmpty()) {
            handleDataPacket(outgoingPackets.poll());
        }

        while (!incomingPackets.isEmpty()) {
            handleIncomingPacket(incomingPackets.poll());
        }

        ticking.set(false);
    }

    public void removeAllPlayers() {
        PlayerListPacket pk = new PlayerListPacket();
        pk.type = PlayerListPacket.TYPE_REMOVE;
        List<PlayerListPacket.Entry> entries = new ArrayList<>();
        for (UUID uid : playerList) {
            entries.add(new PlayerListPacket.Entry(uid));
        }
        playerList.clear();

        pk.entries = entries.toArray(new PlayerListPacket.Entry[0]);
        this.sendDataPacket(pk);
    }

    public void despawnEntities() {
        if (this.spawnedEntities.isEmpty())
            return;

        DataPacket[] packets = spawnedEntities.stream().map((id) -> {
            RemoveEntityPacket rpk = new RemoveEntityPacket();
            rpk.eid = id;

            return rpk;
        }).toArray(DataPacket[]::new);
        this.spawnedEntities.clear();

        getServer().batchPackets(new Player[]{this}, packets);
    }

    public void removeScoreboards() {
        if (scoreboards.isEmpty()) {
            return;
        }

        Set<String> objectives = this.scoreboards.keySet();
        List<ScoreInfo> infos = new ArrayList<>();

        scoreboards.forEach((obj, inf) -> {
            for (Long scoreId : inf) {
                infos.add(new ScoreInfo(scoreId, obj, 1, "a"));
            }
        });

        SetScorePacket ssp = new SetScorePacket();
        ssp.action = SetScorePacket.Action.REMOVE;
        ssp.infos = infos;

        this.sendDataPacket(ssp);

        objectives.forEach(obj -> {
            RemoveObjectivePacket rop = new RemoveObjectivePacket();
            rop.objective = obj;

            this.sendDataPacket(rop);
        });

        scoreboards.clear();
    }

    public void transfer(Client client) {
        PlayerTransferEvent ev;
        this.server.getPluginManager().callEvent(ev = new PlayerTransferEvent(this, client));
        if (!ev.isCancelled()) {
            if (this.client != null) {
                this.client.removePlayer(this, "Player has been transferred");
                this.removeAllPlayers();
                this.despawnEntities();
                this.removeScoreboards();
            }

            this.client = ev.getTargetClient();
            this.client.addPlayer(this);
            PlayerLoginPacket pk = new PlayerLoginPacket();
            pk.uuid = this.uuid;
            pk.address = this.ip;
            pk.port = this.port;
            pk.isFirstTime = this.isFirstTimeLogin;
            pk.cachedLoginPacket = this.cachedLoginPacket;
            pk.protocol = this.getProtocol();

            this.client.sendDataPacket(pk);

            this.isFirstTimeLogin = false;

            this.server.getLogger().info(this.name + " has been transferred to " + this.client.getDescription());
            this.server.updateClientData();
        }
    }

    public void sendDataPacket(DataPacket pk) {
        this.sendDataPacket(pk, false);
    }

    public void sendDataPacket(DataPacket pk, boolean direct) {
        this.sendDataPacket(pk, direct, false);
    }

    public void sendDataPacket(DataPacket pk, boolean direct, boolean needACK) {
        this.interfaz.putPacket(this, pk, needACK, direct);
    }

    public int getPing() {
        return this.interfaz.getNetworkLatency(this);
    }

    public void close() {
        this.close("Generic Reason");
    }

    public void close(String reason) {
        this.close(reason, true);
    }

    public void close(String reason, boolean notify) {
        if (!this.closed) {
            if (notify && reason.length() > 0) {
                DisconnectPacket pk = new DisconnectPacket();
                pk.hideDisconnectionScreen = false;
                pk.message = reason;
                this.sendDataPacket(pk, true);
            }

            this.server.getPluginManager().callEvent(new PlayerLogoutEvent(this));
            this.closed = true;

            if (this.client != null) {
                PlayerLogoutPacket pk = new PlayerLogoutPacket();
                pk.uuid = this.uuid;
                pk.reason = reason;
                this.client.sendDataPacket(pk);
                this.client.removePlayer(this);
            }

            this.server.getLogger().info(this.getServer().getLanguage().translateString("nemisys.player.logOut", TextFormat.AQUA + this.getName() + TextFormat.WHITE,
                    this.ip,
                    String.valueOf(this.port),
                    this.getServer().getLanguage().translateString(reason)));

            this.perm = null;

            this.interfaz.close(this, notify ? reason : "");
            this.getServer().removePlayer(this);
        }
    }

    public void sendMessage(String message) {
        TextPacket pk = new TextPacket();
        pk.type = TextPacket.TYPE_RAW;
        pk.message = this.server.getLanguage().translateString(message);

        this.sendDataPacket(pk);
    }

    @Override
    public void sendMessage(TextContainer message) {
        if (message instanceof TranslationContainer) {
            this.sendTranslation(message.getText(), ((TranslationContainer) message).getParameters());
            return;
        }

        this.sendMessage(message.getText());
    }

    public void sendTranslation(String message, String[] parameters) {
        TextPacket pk = new TextPacket();
        if (!this.server.isLanguageForced()) {
            pk.type = TextPacket.TYPE_TRANSLATION;
            pk.message = this.server.getLanguage().translateString(message, parameters, "nemisys.");
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = this.server.getLanguage().translateString(parameters[i], parameters, "nemisys.");

            }
            pk.parameters = parameters;
        } else {
            pk.type = TextPacket.TYPE_RAW;
            pk.message = this.server.getLanguage().translateString(message, parameters);
        }

        this.sendDataPacket(pk);
    }

    public int rawHashCode() {
        return super.hashCode();
    }

    public void addIncomingPacket(DataPacket pk, boolean direct) {
        this.incomingPackets.offer(pk);
    }

    public void addOutgoingPacket(DataPacket pk) {
        if(Nemisys.DEEP_DEBUG) {
            MainLogger.getLogger().info("(QUEUE OUTGOING) - "+pk.getClass().getSimpleName());
        }
        this.outgoingPackets.offer(pk);
    }

    public boolean canTick() {
        return !this.ticking.get();
    }

    protected void processIncomingBatch(BatchPacket packet) {
        try {
            @Cleanup(value = "release") ByteBuf buf0 = Unpooled.wrappedBuffer(packet.payload);
            @Cleanup(value = "release") ByteBuf buf = CompressionUtil.zlibInflate(buf0);

            byte[] payload = new byte[buf.readableBytes()];
            buf.readBytes(payload);

            BinaryStream buffer = new BinaryStream(payload);
            List<DataPacket> packets = new ArrayList<>();

            while (!buffer.feof()) {
                byte[] data = buffer.getByteArray();

                DataPacket pk = getServer().getNetwork().getPacket(data[0]);

                if (pk != null) {
                    pk.setBuffer(data, protocolGroup.getBufferOffset());
                    pk.decode(getProtocolGroup());
                    pk.isEncoded = true;

                    packets.add(pk);
                }
            }

            for (DataPacket dataPacket : packets) {
                handleIncomingPacket(dataPacket);
            }
        } catch (Exception e) {
            MainLogger.getLogger().logException(e);
        }
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    @Override
    public boolean isOp() {
        return false;
    }

    @Override
    public void setOp(boolean value) {
        /*if (value == this.isOp()) {
            return;
        }

        if (value) {
            this.server.addOp(this.getName());
        } else {
            this.server.removeOp(this.getName());
        }

        this.recalculatePermissions();*/
        //this.sendCommandData();
    }

    @Override
    public boolean isPermissionSet(String name) {
        return this.perm.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return this.perm.isPermissionSet(permission);
    }

    @Override
    public boolean hasPermission(String name) {
        return this.perm != null && this.perm.hasPermission(name);
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return this.perm.hasPermission(permission);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        return this.addAttachment(plugin, null);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name) {
        return this.addAttachment(plugin, name, null);
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, Boolean value) {
        return this.perm.addAttachment(plugin, name, value);
    }

    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        this.perm.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        this.server.getPluginManager().unsubscribeFromPermission(Server.BROADCAST_CHANNEL_USERS, this);
        this.server.getPluginManager().unsubscribeFromPermission(Server.BROADCAST_CHANNEL_ADMINISTRATIVE, this);

        if (this.perm == null) {
            return;
        }

        this.perm.recalculatePermissions();

        if (this.hasPermission(Server.BROADCAST_CHANNEL_USERS)) {
            this.server.getPluginManager().subscribeToPermission(Server.BROADCAST_CHANNEL_USERS, this);
        }

        if (this.hasPermission(Server.BROADCAST_CHANNEL_ADMINISTRATIVE)) {
            this.server.getPluginManager().subscribeToPermission(Server.BROADCAST_CHANNEL_ADMINISTRATIVE, this);
        }

        this.sendCommandData();
    }

    @Override
    public Map<String, PermissionAttachmentInfo> getEffectivePermissions() {
        return this.perm.getEffectivePermissions();
    }

    public void sendCommandData() {
        AvailableCommandsPacket pk = new AvailableCommandsPacket();
        Map<String, CommandDataVersions> data = new HashMap<>(this.clientCommands);

        for (Command command : getServer().getCommandMap().getCommands().values()) {
            if (!command.isGlobal() || !command.testPermissionSilent(this)) {
                continue;
            }

            CommandDataVersions data0 = command.generateCustomCommandData(this);
            if (data0 != null) {
                data.put(command.getName(), data0);
            }
        }

        //TODO: structure checking
        pk.commands = data;

        pk.encode(getProtocolGroup());
        pk.isEncoded = true;

        this.sendDataPacket(pk);
    }
}
