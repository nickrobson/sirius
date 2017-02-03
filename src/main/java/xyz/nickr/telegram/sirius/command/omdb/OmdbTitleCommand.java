package xyz.nickr.telegram.sirius.command.omdb;

import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import xyz.nickr.jomdb.model.TitleResult;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;

/**
 * @author Nick Robson
 */
public class OmdbTitleCommand extends Command {

    public OmdbTitleCommand() {
        super("omdbtitle");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        if (args.length != 1) {
            sendUsage(message);
        } else {
            Series series;
            try {
                series = Sirius.getSeriesController().getSeriesByLink(args[0]);
                if (series == null) {
                    series = Sirius.getSeriesController().getSeries(args[0], false);
                }
                if (series == null) {
                    series = new Series(args[0]);
                }
            } catch (Exception ex) {
                message.getChat().sendMessage(SendableTextMessage.plain("That's not a valid series.").replyTo(message).build());
                return;
            }
            message.getChat().sendMessage(SendableTextMessage.builder()
                    .message(
                            "*" + escape(series.getName()) + "*" + escape( " (" + series.getYear() + "), ") + "_" + escape(series.getId()) + "_" + escape(", " + indefinite(series.getGenre()) + " " + series.getGenre() + " " + series.getType()) + "\n" +
                                    (series.getRating().equals("N/A") || series.getVotes().equals("N/A") ? "" : "_Rating:_ " + escape(series.getRating() + " from " + series.getVotes() + " votes") + "\n") +
                                    (series.getMetascore().equals("N/A") ? "" : "_Metascore:_ " + escape(series.getMetascore()) + "\n") +
                                    (series.getRuntime().equals("N/A") ? "" : "_Runtime:_ " + escape(series.getRuntime()) + "\n") +
                                    (series.getDirector().equals("N/A") ? "" : "_Director:_ " + escape(series.getDirector()) + "\n") +
                                    (series.getWriter().equals("N/A") ? "" : "_Writer:_ " + escape(series.getWriter()) + "\n") +
                                    (series.getActors().equals("N/A") ? "" : "_Actors:_ " + escape(series.getActors()) + "\n") +
                                    (series.getAwards().equals("N/A") ? "" : "_Awards:_ " + escape(series.getAwards()) + "\n") +
                                    (series.getLanguage().equals("N/A") ? "" : "_Language:_ " + escape(series.getLanguage()) + "\n") +
                                    (series.getCountry().equals("N/A") ? "" : "_Country:_ " + escape(series.getCountry()) + "\n") +
                                    (series.getPlot().equals("N/A") ? "" : "_Plot:_ " + escape(series.getPlot()))
                    )
                    .parseMode(ParseMode.MARKDOWN)
                    .replyTo(message)
                    .build());
        }
    }

    private String indefinite(String s) {
        return indefinite(s, true);
    }

    private String indefinite(String s, boolean careAboutH) {
        if ((s = s.toLowerCase()).isEmpty())
            return "a";
        char c = s.charAt(0);
        if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')
            return "an";
        if (careAboutH && c == 'h')
            return indefinite(s.substring(1), false);
        return "a";
    }

}
