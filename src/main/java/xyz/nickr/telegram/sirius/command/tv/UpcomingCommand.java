package xyz.nickr.telegram.sirius.command.tv;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import xyz.nickr.telegram.sirius.Sirius;
import xyz.nickr.telegram.sirius.tv.Episode;
import xyz.nickr.telegram.sirius.tv.Season;
import xyz.nickr.telegram.sirius.tv.Series;
import xyz.nickr.telepad.TelepadBot;
import xyz.nickr.telepad.command.Command;
import xyz.nickr.telepad.util.PaginatedData;

/**
 * @author Nick Robson
 */
public class UpcomingCommand extends Command {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("E, d MMM ''uu");

    public UpcomingCommand() {
        super("upcoming");
        this.setHelp("gets when new episodes are coming out of shows");
    }

    @Override
    public void exec(TelepadBot bot, Message message, String[] args) {
        LocalDateTime now = LocalDateTime.now().minus(2, ChronoUnit.DAYS);
        LocalDateTime cutOff = now.plus(32, ChronoUnit.DAYS);
        Map<LocalDateTime, List<Series>> seriesByDay = new TreeMap<>();
        for (Series series : Sirius.getSeriesController().getSeries()) {
            Map.Entry<Season, Episode> nextEpisode = series.getNextAiredEpisode();
            if (nextEpisode == null)
                continue;
            LocalDateTime releaseDate = nextEpisode.getValue().getReleaseDate();
            if (releaseDate == null || releaseDate.isBefore(now) || releaseDate.isAfter(cutOff))
                continue;
            seriesByDay.merge(releaseDate, Collections.singletonList(series), this::mergeLists);
        }

        DateTimeFormatter formatter = FORMATTER.withLocale(bot.getLocale());

        List<String> pages = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<Series>> entry : seriesByDay.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String dateDisplay = entry.getKey().format(formatter);
                entry.getValue()
                        .sort(Comparator.comparing(
                                Series::getName,
                                String.CASE_INSENSITIVE_ORDER)
                        );

                List<String> names = entry.getValue()
                        .stream()
                        .map(Series::getName)
                        .map(Command::escape)
                        .collect(Collectors.toList());

                pages.add(
                        "*" + escape(dateDisplay) + "*\n" +
                        String.join(", ", names)
                );
            }
        }

        PaginatedData paginatedData = new PaginatedData(pages);
        paginatedData.setParseMode(ParseMode.MARKDOWN);
        paginatedData.send(0, message);
    }

    private <T> List<T> mergeLists(List<T> a, List<T> b) {
        List<T> c = new ArrayList<>(a);
        c.addAll(b);
        return c;
    }

}
