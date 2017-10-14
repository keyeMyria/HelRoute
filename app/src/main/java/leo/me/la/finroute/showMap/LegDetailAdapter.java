package leo.me.la.finroute.showMap;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import leo.me.la.finroute.R;
import leo.me.la.finroute.RouteQuery;
import leo.me.la.finroute.root.Utils;
import leo.me.la.finroute.type.Mode;

class LegDetailAdapter extends RecyclerView.Adapter<LegDetailAdapter.LegVH> implements StopVisibilityListener {
    private Context context;
    private List<RouteQuery.Leg> legs;
    private SparseBooleanArray state;
    private SparseArray<StopAdapter> childStopAdapters;
    LegDetailAdapter(Context context, List<RouteQuery.Leg> legs) {
        this.context = context;
        this.legs = legs;
        state = new SparseBooleanArray();
        childStopAdapters = new SparseArray<>();
    }


    @Override
    public int getItemViewType(int position) {
        return (legs.get(position).mode() == Mode.WALK) ? 0 : 1;
    }

    @Override
    public LegVH onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout = (viewType == 0) ? R.layout.item_leg_walk : R.layout.item_leg_transport;
        View itemView = LayoutInflater.from(context).inflate(layout, parent, false);
        return new LegVH(itemView, this);
    }

    @Override
    public void onBindViewHolder(final LegVH holder, int position) {
        holder.setPosition(position);
        RouteQuery.Leg prev = (position == 0) ? null : legs.get(position - 1);
        holder.bind(legs.get(position), prev);
    }

    @Override
    public int getItemCount() {
        return legs.size();
    }

    @Override
    public void onVisibilityChange(int position, boolean isShown) {
        state.put(position, isShown);
    }

    @Override
    public boolean currentVisibility(int position) {
        return state.get(position, false);
    }

    @Override
    public void update(int position) {
        notifyItemChanged(position);
    }

    @Override
    public StopAdapter getChildAdapter(int position) {
        return childStopAdapters.get(position);
    }

    @Override
    public void setChildAdapter(int position, StopAdapter adapter) {
        childStopAdapters.put(position, adapter);
    }

    static class LegVH extends RecyclerView.ViewHolder {
        @BindView(R.id.transfer)
        @Nullable
        View transfer;
        @BindView(R.id.tvStartTransfer)
        @Nullable
        TextView tvStartTransfer;
        @BindView(R.id.tvFromTransfer)
        @Nullable
        TextView tvFromTransfer;
        @BindView(R.id.tvStopCodeTransfer)
        @Nullable
        TextView tvStopCodeTransfer;
        @BindView(R.id.tvPlatformTransfer)
        @Nullable
        TextView tvPlatformTransfer;
        @BindView(R.id.tvContentTransfer)
        @Nullable
        TextView tvContentTransfer;
        @BindView(R.id.tvCode)
        @Nullable
        TextView tvCode;
        @BindView(R.id.rcvStop)
        @Nullable
        RecyclerView rcvStop;
        @BindView(R.id.tvStartTime)
        TextView tvStartTime;
        @BindView(R.id.tvStopCode)
        TextView tvStopCode;
        @BindView(R.id.tvPlatform)
        TextView tvPlatform;
        @BindView(R.id.tvContent)
        TextView tvContent;
        @BindView(R.id.tvFrom)
        TextView tvFrom;
        @BindView(R.id.imgFrom)
        ImageView imgFrom;
        @BindView(R.id.view)
        View view;
        @BindView(R.id.destination)
        View destination;
        @BindView(R.id.tvEndTime)
        TextView tvEndTime;
        @BindView(R.id.tvDuration)
        TextView tvDuration;
        @Nullable
        @BindView(R.id.tvShowHide)
        TextView tvShowHide;

        private int position;
        private StopVisibilityListener listener;

        LegVH(View itemView, @NonNull StopVisibilityListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.listener = listener;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        @Optional
        @OnClick(R.id.tvShowHide)
        void onContentClick() {
            if (rcvStop != null && tvShowHide != null) {
                listener.onVisibilityChange(position, !listener.currentVisibility(position));
                if (listener.currentVisibility(position)) {
                    rcvStop.setVisibility(View.VISIBLE);
                    Animation slideDown = AnimationUtils.loadAnimation(itemView.getContext(),
                            R.anim.slide_down);
                    rcvStop.startAnimation(slideDown);
                    tvShowHide.setText(itemView.getContext().getString(R.string.hide));
                    tvShowHide.setBackgroundResource(R.drawable.bg_hide);
                } else {
                    Animation slideUp = AnimationUtils.loadAnimation(itemView.getContext(),
                            R.anim.slide_up);
                    rcvStop.startAnimation(slideUp);
                    slideUp.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            listener.update(position);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }
            }
        }

        public void bind(RouteQuery.Leg leg, RouteQuery.Leg prev) {
            if (transfer != null) {
                bindTransfer(leg, prev);
            }
            bindInfo(leg);
            if (leg.mode() != null && leg.mode() != Mode.WALK && tvCode != null) {
                bindTransportMode(leg);
                bindTransportContent(leg);
            } else if (leg.mode() != null && leg.mode() == Mode.WALK) {
                bindWalkContent(leg);
            }
            bindOriginDestination(leg);
        }

        private void bindTransfer(RouteQuery.Leg leg, RouteQuery.Leg prev) {
            boolean isTransfer = prev != null
                    && prev.to().stop() != null
                    && leg.from().stop() != null
                    && prev.to().stop().code() != null
                    && leg.from().stop().code() != null
                    && prev.to().stop().code().equals(leg.from().stop().code());
            if (isTransfer && transfer != null) {
                transfer.setVisibility(View.VISIBLE);
                if (tvStartTransfer != null && prev.endTime() != null)
                    tvStartTransfer.setText(Utils.getShortTimeFormat().format(prev.endTime()));
                if (tvFromTransfer != null && prev.to().stop() != null) {
                    tvFromTransfer.setText(prev.to().stop().name());
                    tvFromTransfer.setSelected(true);
                }
                if (tvStopCodeTransfer != null) {
                    tvStopCodeTransfer.setText(prev.to().stop().code());
                }
                if (tvPlatformTransfer != null) {
                    if (prev.to().stop().platformCode() != null) {
                        tvPlatformTransfer.setText(itemView.getContext().getString(R.string.platform) + " " +
                                prev.to().stop().platformCode());
                        tvPlatformTransfer.setVisibility(View.VISIBLE);
                    } else
                        tvPlatformTransfer.setVisibility(View.GONE);
                }
                if (leg.startTime() != null && prev.endTime() != null
                        && tvContentTransfer != null) {
                    Long time = (leg.startTime() - prev.endTime()) / 1000;
                    if (time >= 60)
                        tvContentTransfer.setText(
                                itemView.getContext().getString(R.string.wait) + " " +
                                        Utils.getReadableDuration(itemView.getContext(), time));
                    else
                        transfer.setVisibility(View.GONE);
                }
            } else if (transfer != null) {
                transfer.setVisibility(View.GONE);
            }
        }

        private void bindInfo(RouteQuery.Leg leg) {
            tvStartTime.setText(Utils.getShortTimeFormat().format(leg.startTime()));
            tvFrom.setText(leg.from().name());
            tvFrom.setSelected(true);
            if (leg.from().stop() != null && leg.from().stop().code() != null) {
                tvStopCode.setVisibility(View.VISIBLE);
                tvStopCode.setText(leg.from().stop().code());
            } else
                tvStopCode.setVisibility(View.GONE);
            if (leg.from().stop() != null && leg.from().stop().platformCode() != null) {
                tvPlatform.setVisibility(View.VISIBLE);
                tvPlatform.setText(itemView.getContext().getString(R.string.platform) + " " + leg.from().stop().platformCode());
            } else
                tvPlatform.setVisibility(View.GONE);
        }

        private void bindTransportMode(RouteQuery.Leg leg) {
            int imgResource = Utils.getModeDrawableId(leg.mode());
            int progress = Utils.getModeProgressId(leg.mode());
            int fromIcon = Utils.getModeFromIcon(leg.mode());
            int colorId = Utils.getModeColorId(leg.mode());
            tvCode.setCompoundDrawablesWithIntrinsicBounds(0, imgResource, 0, 0);
            tvCode.setTextColor(ContextCompat.getColor(itemView.getContext(), colorId));
            if (leg.route() != null && leg.route().shortName() != null)
                tvCode.setText(leg.route().shortName());
            view.setBackgroundResource(progress);
            imgFrom.setImageResource(fromIcon);
        }

        private void bindTransportContent(RouteQuery.Leg leg) {
            String time;
            String stops;
            if (leg.distance() != null && leg.endTime() != null && leg.startTime() != null)
                time = "(" + Utils.getReadableDuration(itemView.getContext(),
                        (leg.endTime() - leg.startTime()) / 1000) + ")";
            else
                time = "";
            if (leg.route() != null && leg.route().stops() != null) {
                int from = 0, to = 0;
                if (leg.from().stop() != null && leg.from().stop().code() != null)
                    from = getFromStopPosition(leg.from().stop().code(), leg.route().stops());
                if (leg.to().stop() != null && leg.to().stop().code() != null)
                    to = getToStopPosition(leg.to().stop().code(), leg.route().stops());
                if (from >= 0 && to >= 0 && Math.abs(to - from) > 1) {
                    int numberOfStop = Math.abs(to - from) - 1;
                    stops = numberOfStop + " " + itemView.getContext().getString(R.string.stop);
                } else {
                    stops = itemView.getContext().getString(R.string.no_stop);
                }
                bindStops(leg.route().stops(), from, to);
            } else
                stops = itemView.getContext().getString(R.string.no_stop);
            tvContent.setText(stops);
            tvDuration.setText(time);
            if (tvShowHide != null && stops.equals(itemView.getContext().getString(R.string.no_stop))) {
                tvShowHide.setVisibility(View.GONE);
            } else if (tvShowHide != null && !stops.equals(itemView.getContext().getString(R.string.no_stop))) {
                tvShowHide.setVisibility(View.VISIBLE);
            }
        }

        private void bindStops(List<RouteQuery.Stop> stops, int from, int to) {
            if (rcvStop != null && tvShowHide != null) {
                if (listener.getChildAdapter(position) == null) {
                    listener.setChildAdapter(position, new StopAdapter(itemView.getContext(), stops, from, to));
                }
                rcvStop.swapAdapter(listener.getChildAdapter(position), true);
//                rcvStop.setVisibility(View.GONE);
                rcvStop.setVisibility(listener.currentVisibility(position) ? View.VISIBLE : View.GONE);
                tvShowHide.setText(listener.currentVisibility(position)
                        ? itemView.getContext().getString(R.string.hide)
                        : itemView.getContext().getString(R.string.show));
                tvShowHide.setBackgroundResource(listener.currentVisibility(position) ? R.drawable.bg_hide : R.drawable.bg_show);
            }
        }

        private void bindWalkContent(RouteQuery.Leg leg) {
            String time;
            String distance;
            imgFrom.setImageResource(R.drawable.ic_origin_walk);
            if (leg.distance() != null && leg.endTime() != null && leg.startTime() != null)
                time = "(" + Utils.getReadableDuration(itemView.getContext(),
                        (leg.endTime() - leg.startTime()) / 1000) + ")";
            else
                time = "";
            if (leg.distance() != null)
                distance = itemView.getContext().getString(R.string.walk) + " "
                        + Utils.getReadableDistance(leg.distance());
            else
                distance = "";
            tvContent.setText(distance);
            tvDuration.setText(time);
        }

        private void bindOriginDestination(RouteQuery.Leg leg) {
            if (leg.from().name() != null && leg.from().name().equals("Origin")) {
                imgFrom.setImageResource(R.drawable.ic_origin_mark);
            }
            if (leg.to().name() != null && leg.to().name().equals("Destination")) {
                destination.setVisibility(View.VISIBLE);
                if (leg.endTime() != null)
                    tvEndTime.setText(Utils.getShortTimeFormat().format(leg.endTime()));
            } else {
                destination.setVisibility(View.GONE);
            }
        }

        private int getFromStopPosition(String code, List<RouteQuery.Stop> stops) {
            for (int i = 0; i < stops.size(); i++) {
                if (code.equals(stops.get(i).code()))
                    return i;
            }
            return -1;
        }

        private int getToStopPosition(String code, List<RouteQuery.Stop> stops) {
            for (int i = stops.size() - 1; i >= 0; i--) {
                if (code.equals(stops.get(i).code()))
                    return i;
            }
            return -1;
        }
    }
}