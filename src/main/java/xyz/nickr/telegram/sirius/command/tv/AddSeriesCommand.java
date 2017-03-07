package xyz.nickr.telegram.sirius.command.tv;

import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.filmfo.Filmfo;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class AddSeriesCommand extends Command {

    public AddSeriesCommand() {
        super("addseries");
        this.setHelp("adds a series to the list of tracked series");
        this.setUsage("[imdb id] [name]");
        this.setPermission("series.add");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length != 2) {
            sendUsage(message);
        } else {
            args[0] = args[0].toLowerCase(bot.getLocale());
            if (!Filmfo.IMDB_ID_PATTERN.matcher(args[0]).matches()) {
                message.getChat().sendMessage(SendableTextMessage.plain("That isn't a valid IMDB ID!").replyTo(message).build());
                return;
            }
            Series series;
            try {
                series = Sirius.getSeriesController().getSeries(args[0], true);
            } catch (Exception ex) {
                message.getChat().sendMessage(SendableTextMessage.plain("That either isn't a series or it isn't a valid ID!").replyTo(message).build());
                return;
            }
            if (Sirius.getSeriesController().addShow(series, args[1])) {
                message.getChat().sendMessage(SendableTextMessage.plain("Successfully added " + escape(series.getName()) + "!").replyTo(message).build());
            } else {
                message.getChat().sendMessage(SendableTextMessage.plain(escape(series.getName()) + " is already in the database!").replyTo(message).build());
            }
        }
    }

}
